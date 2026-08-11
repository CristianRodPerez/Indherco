package com.indherco.postes.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class ProductionConfigurationTest {

    @Test
    void productionProfileUsesFlywayAndExposesOnlyHealth() throws IOException {
        var propertySources = new YamlPropertySourceLoader().load(
            "application-prod",
            new ClassPathResource("application-prod.yml")
        );
        var properties = propertySources.getFirst();

        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(properties.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(properties.getProperty("springdoc.api-docs.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false);
    }
}
