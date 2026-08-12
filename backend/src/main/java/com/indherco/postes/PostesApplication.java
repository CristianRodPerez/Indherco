package com.indherco.postes;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PostesApplication {

    public static void main(String[] args) {
        // Los TIMESTAMP de la aplicacion representan la hora civil de la planta.
        // Render y otros proveedores ejecutan la JVM en UTC si no se configura.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
        SpringApplication.run(PostesApplication.class, args);
    }
}
