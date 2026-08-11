package com.indherco.postes.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI indhercoOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Indherco Control de Stock API")
                .description("API interna para produccion, despacho, consumo, stock e inventario de oficina.")
                .version("0.1.0")
                .license(new License().name("Uso interno")));
    }
}
