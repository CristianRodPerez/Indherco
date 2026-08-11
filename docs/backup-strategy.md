# Estrategia de respaldo MySQL

## Recomendacion

- Ejecutar backup diario fuera del horario de trabajo.
- Mantener retencion local minima de 7 a 14 dias.
- Copiar respaldos importantes a almacenamiento externo.
- Probar restauracion de forma periodica.

## Backup

Usar:

```powershell
.\scripts\backup-mysql.ps1
```

Variables requeridas:

- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_NAME`

Opcional:

- `BACKUP_DIR`

## Restore

Usar:

```powershell
.\scripts\restore-mysql.ps1 -BackupFile "C:\ruta\archivo.sql"
```

Nunca restaurar sobre una base productiva sin respaldo previo.
