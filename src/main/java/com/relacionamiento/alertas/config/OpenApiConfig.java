package com.relacionamiento.alertas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API REST - Sistema de Alertas de Monitoreo")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Dirección de Relacionamiento - Inteligencia de Mercados")
                                .email("relacionamiento@institucion.edu.co")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Entorno Local de Desarrollo"),
                        new Server().url("https://api.relacionamiento.edu.co").description("Servidor de Producción")
                ));
    }
}
