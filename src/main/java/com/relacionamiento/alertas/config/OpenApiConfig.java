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
                        .description("API REST diseñada para las 3 herramientas de Inteligencia de Mercados de la Dirección de Relacionamiento: " +
                                "1) Empresas Objetivo, 2) SECOP II y Cooperación Internacional, 3) Fondo de Recuperación Post-Terremoto. " +
                                "Incluye integración con WhatsApp Cloud API / Webhook para consultas y notificaciones.")
                        .contact(new Contact()
                                .name("Dirección de Relacionamiento - Inteligencia de Mercados")
                                .email("relacionamiento@institucion.edu.co")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Entorno Local de Desarrollo"),
                        new Server().url("https://api.relacionamiento.edu.co").description("Servidor de Producción")
                ));
    }
}
