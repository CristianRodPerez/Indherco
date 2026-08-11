# Despliegue de Indherco en AWS Lightsail

Esta guia instala Indherco en una sola instancia Ubuntu. La instancia ejecuta:

- Nginx para servir React/PWA y terminar HTTPS.
- Spring Boot como servicio privado en `127.0.0.1:8080`.
- MySQL 8 como base de datos local y privada.
- Un respaldo MySQL diario con retencion de 14 dias.

No se requiere Docker, Kubernetes, RDS ni balanceador.

## 1. Requisitos y datos necesarios

Antes de comenzar, definir:

- `IP_PUBLICA`: IP estatica de Lightsail.
- `DOMINIO`: por ejemplo `app.indherco.cl`.
- `EMAIL`: correo para avisos de Let's Encrypt.
- Una clave fuerte para el usuario MySQL de la aplicacion.
- Una clave fuerte para el usuario MySQL de respaldos.
- Una clave fuerte para el administrador inicial.

Se recomienda una instancia Lightsail con Ubuntu 24.04 LTS y al menos 2 GB de RAM. MySQL y Java comparten la misma maquina, por lo que 1 GB puede quedar justo.

## 2. Crear la instancia Lightsail

1. En AWS Lightsail, crear una instancia Linux con Ubuntu 24.04 LTS.
2. Asociar una IP estatica. La IP temporal cambia al detener y volver a iniciar la instancia.
3. En la red de Lightsail, permitir TCP `22`, `80` y `443`.
4. Si es posible, limitar el puerto `22` a la IP desde donde se administra.
5. No abrir los puertos `3306` ni `8080`.
6. Crear en el proveedor DNS un registro `A` para `DOMINIO` apuntando a `IP_PUBLICA`.

Esperar a que el dominio responda con la nueva IP antes de solicitar el certificado HTTPS.

## 3. Conectarse por SSH

Desde el computador local:

```bash
ssh -i /ruta/clave.pem ubuntu@IP_PUBLICA
```

Lightsail tambien permite abrir una terminal SSH desde el navegador.

## 4. Preparar Ubuntu

En el servidor:

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y openjdk-21-jre-headless mysql-server nginx certbot python3-certbot-nginx rsync curl
sudo timedatectl set-timezone America/Santiago
java -version
mysql --version
nginx -v
```

Habilitar los servicios base:

```bash
sudo systemctl enable --now mysql
sudo systemctl enable --now nginx
```

Configurar tambien el firewall local. Primero se permite SSH para evitar perder la conexion:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
sudo ufw status
```

## 5. Crear la base de datos

Generar dos claves aleatorias y guardarlas en un administrador de contrasenas:

```bash
openssl rand -base64 36
openssl rand -base64 36
```

Abrir MySQL como administrador local:

```bash
sudo mysql
```

Ejecutar lo siguiente, reemplazando ambas claves:

```sql
CREATE DATABASE indherco
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'indherco_app'@'127.0.0.1'
  IDENTIFIED BY 'CLAVE_SEGURA_APLICACION';

GRANT ALL PRIVILEGES ON indherco.*
  TO 'indherco_app'@'127.0.0.1';

CREATE USER 'indherco_backup'@'127.0.0.1'
  IDENTIFIED BY 'CLAVE_SEGURA_RESPALDO';

GRANT SELECT, SHOW VIEW, TRIGGER, LOCK TABLES ON indherco.*
  TO 'indherco_backup'@'127.0.0.1';

FLUSH PRIVILEGES;
EXIT;
```

El usuario `indherco_app` tiene permisos solo sobre la base `indherco`. Se requieren permisos de esquema porque Flyway crea y actualiza las tablas al iniciar nuevas versiones. La aplicacion nunca usa `root`.

Probar la conexion:

```bash
mysql -h 127.0.0.1 -u indherco_app -p indherco -e "SELECT 1;"
```

## 6. Crear el usuario y las carpetas de la aplicacion

```bash
sudo adduser --system --group --home /opt/indherco --no-create-home indherco
sudo install -d -m 0750 -o root -g indherco /opt/indherco/backend
sudo install -d -m 0750 -o root -g indherco /etc/indherco
sudo install -d -m 0755 -o root -g root /var/www/indherco
```

El usuario `indherco` no tiene inicio de sesion y se usa unicamente para ejecutar Spring Boot.

## 7. Compilar la aplicacion

Se recomienda compilar en el computador de desarrollo y subir solo los artefactos. El servidor no necesita Maven ni Node.js.

Backend:

```bash
cd backend
mvn clean verify
cd ..
```

Esto ejecuta los tests unitarios y de integracion con MySQL Testcontainers, y crea `backend/target/postes-0.0.1-SNAPSHOT.jar`. Docker debe estar iniciado en el computador de desarrollo.

Frontend:

```bash
cd frontend
npm ci
npm run build
cd ..
```

El build usa `VITE_API_BASE_URL=/api` desde `frontend/.env.production`. La direccion real la resuelve Nginx, por lo que el JavaScript productivo no depende de `localhost`.

Empaquetar desde la raiz del proyecto:

```bash
tar -czf indherco-release.tar.gz backend/target/postes-0.0.1-SNAPSHOT.jar frontend/dist deployment DEPLOYMENT.md
scp -i /ruta/clave.pem indherco-release.tar.gz ubuntu@IP_PUBLICA:~/
```

En el servidor:

```bash
rm -rf ~/indherco-release
mkdir -p ~/indherco-release
tar -xzf ~/indherco-release.tar.gz -C ~/indherco-release
cd ~/indherco-release
chmod +x deployment/scripts/*.sh
```

## 8. Configurar variables del backend

Copiar la plantilla:

```bash
sudo cp deployment/env/indherco.env.example /etc/indherco/indherco.env
sudo chown root:indherco /etc/indherco/indherco.env
sudo chmod 0640 /etc/indherco/indherco.env
sudo nano /etc/indherco/indherco.env
```

Generar el secreto JWT:

```bash
openssl rand -base64 48
```

En `/etc/indherco/indherco.env` reemplazar obligatoriamente:

- `DB_PASSWORD` por la clave de `indherco_app`.
- `JWT_SECRET` por el valor aleatorio generado. Debe tener al menos 32 caracteres.
- `ADMIN_PASSWORD` por una clave inicial fuerte.
- `CORS_ALLOWED_ORIGINS` por `https://DOMINIO`, sin `/` al final.

Las comillas permiten usar caracteres especiales. El archivo solo puede ser leido por `root` y por el grupo del servicio.

## 9. Instalar el servicio backend

```bash
sudo cp deployment/systemd/indherco.service /etc/systemd/system/indherco.service
sudo systemctl daemon-reload
sudo systemctl enable indherco
./deployment/scripts/deploy-backend.sh
```

En el primer inicio, Flyway ejecuta todas las migraciones y crea el esquema. Hibernate solo valida que las entidades coincidan con el esquema; no crea ni modifica tablas.

Comprobar el servicio:

```bash
sudo systemctl status indherco --no-pager
curl http://127.0.0.1:8080/actuator/health
sudo journalctl -u indherco -n 100 --no-pager
```

La respuesta de health esperada es:

```json
{"status":"UP"}
```

## 10. Configurar Nginx y desplegar React

Reemplazar el dominio de ejemplo al instalar la configuracion:

```bash
sudo cp deployment/nginx/indherco.conf /etc/nginx/sites-available/indherco
sudo sed -i 's/indherco.example.com/DOMINIO/g' /etc/nginx/sites-available/indherco
sudo ln -sfn /etc/nginx/sites-available/indherco /etc/nginx/sites-enabled/indherco
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
./deployment/scripts/deploy-frontend.sh
```

En el comando anterior, escribir el dominio real en lugar de la palabra `DOMINIO`.

Probar antes de HTTPS:

```bash
curl -I http://DOMINIO
curl http://DOMINIO/actuator/health
```

Nginx sirve la PWA, envia `/api/*` al backend y permite que las rutas de React funcionen al recargar la pagina.

## 11. Activar HTTPS con Let's Encrypt

El dominio debe apuntar a Lightsail y los puertos `80` y `443` deben estar abiertos.

```bash
sudo certbot --nginx -d DOMINIO --redirect --agree-tos -m EMAIL
```

Certbot agrega el bloque HTTPS y la redireccion desde HTTP a HTTPS. Verificar:

```bash
curl -I https://DOMINIO
curl https://DOMINIO/actuator/health
sudo certbot renew --dry-run
systemctl list-timers | grep certbot
```

No se debe publicar directamente el puerto `8080`; HTTPS termina en Nginx.

## 12. Configurar respaldos diarios

Copiar y completar la configuracion:

```bash
sudo cp deployment/env/backup.env.example /etc/indherco/backup.env
sudo chown root:root /etc/indherco/backup.env
sudo chmod 0600 /etc/indherco/backup.env
sudo nano /etc/indherco/backup.env
```

Reemplazar `MYSQL_BACKUP_PASSWORD` por la clave del usuario `indherco_backup`.

Instalar el script y cron:

```bash
sudo install -m 0750 deployment/scripts/backup-mysql.sh /usr/local/sbin/backup-indherco-mysql
sudo cp deployment/cron/indherco-backup /etc/cron.d/indherco-backup
sudo chmod 0644 /etc/cron.d/indherco-backup
sudo systemctl restart cron
```

Probar inmediatamente:

```bash
sudo /usr/local/sbin/backup-indherco-mysql
sudo ls -lh /var/backups/indherco
sudo gzip -t /var/backups/indherco/*.sql.gz
```

Cron ejecuta el respaldo todos los dias a las `02:15`. Los archivos con mas de 14 dias se eliminan automaticamente. Revisar su registro con:

```bash
sudo tail -n 100 /var/log/indherco-backup.log
```

Conviene copiar periodicamente algun respaldo fuera de la instancia. Los respaldos en el mismo servidor no protegen frente a perdida completa de la instancia.

## 13. Actualizar la aplicacion

### Backend

En el computador de desarrollo:

```bash
cd backend
mvn clean verify
```

Subir el nuevo JAR y ejecutar en el servidor, desde una copia actualizada de `indherco-release`:

```bash
./deployment/scripts/deploy-backend.sh
```

El script guarda el JAR anterior como `/opt/indherco/backend/indherco.jar.previous`, instala el nuevo, reinicia systemd y espera hasta que health responda `UP`.

### Frontend

En el computador de desarrollo:

```bash
cd frontend
npm ci
npm run build
```

Subir `frontend/dist` y ejecutar en el servidor:

```bash
./deployment/scripts/deploy-frontend.sh
```

El script sincroniza la carpeta publicada, comprueba Nginx y recarga el servicio.

## 14. Restaurar un respaldo

Una restauracion reemplaza datos y debe hacerse en una ventana sin usuarios. Primero crear un respaldo adicional.

```bash
sudo /usr/local/sbin/backup-indherco-mysql
sudo systemctl stop indherco
gunzip -c /var/backups/indherco/ARCHIVO.sql.gz | mysql -h 127.0.0.1 -u indherco_app -p indherco
sudo systemctl start indherco
curl http://127.0.0.1:8080/actuator/health
```

## 15. Operacion y diagnostico

Estado y logs del backend:

```bash
sudo systemctl status indherco --no-pager
sudo journalctl -u indherco -f
```

Estado y logs de Nginx:

```bash
sudo nginx -t
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log
```

Estado de MySQL y espacio en disco:

```bash
sudo systemctl status mysql --no-pager
df -h
sudo du -sh /var/backups/indherco
```

Reiniciar componentes:

```bash
sudo systemctl restart indherco
sudo systemctl reload nginx
sudo systemctl restart mysql
```

Evitar reiniciar MySQL durante la jornada salvo que exista una razon concreta.

## 16. Controles de seguridad incluidos

- Los secretos productivos se cargan desde `/etc/indherco/indherco.env`, no desde el JAR ni el repositorio.
- Las contrasenas de usuarios se almacenan con BCrypt.
- JWT valida firma, expiracion y que el usuario siga activo.
- Los endpoints `/api/**` requieren autenticacion, salvo `/api/auth/login`.
- Los permisos de negocio se aplican con roles y permisos en los controladores.
- Actuator solo expone `/actuator/health` y no muestra detalles internos.
- Swagger/OpenAPI queda desactivado en el perfil `prod`.
- CORS acepta unicamente el dominio configurado.
- MySQL y Spring Boot escuchan para uso local; Lightsail solo publica SSH, HTTP y HTTPS.
- systemd reinicia el backend ante una falla y aplica limites basicos al proceso.

## 17. Mantenimiento recomendado

- Revisar una vez por semana `systemctl status`, espacio en disco y respaldos.
- Probar una restauracion en un entorno separado al menos cada algunos meses.
- Aplicar actualizaciones de Ubuntu en una ventana de mantenimiento.
- Crear una instantanea de Lightsail antes de cambios grandes del sistema operativo.
- No enviar por correo o WhatsApp el archivo `/etc/indherco/indherco.env`.
- No agregar reglas publicas para `3306` o `8080`.
