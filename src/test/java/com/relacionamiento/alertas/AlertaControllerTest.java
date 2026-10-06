package com.relacionamiento.alertas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AlertaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/alertas - Debe listar alertas sembradas")
    void testListarAlertas() throws Exception {
        mockMvc.perform(get("/api/v1/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/alertas/metricas - Debe retornar estadísticas agregadas")
    void testObtenerMetricas() throws Exception {
        mockMvc.perform(get("/api/v1/alertas/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAlertas").isNumber())
                .andExpect(jsonPath("$.data.porHerramienta.EMPRESAS").exists())
                .andExpect(jsonPath("$.data.porHerramienta.SECOP_COOPERACION").exists())
                .andExpect(jsonPath("$.data.porHerramienta.FONDO_RECUPERACION").exists());
    }

    @Test
    @DisplayName("GET /api/v1/whatsapp/webhook - Debe verificar challenge de Meta con token válido")
    void testVerificarWebhookExitoso() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "alerta_relacionamiento_token_2026")
                        .param("hub.challenge", "1234567890"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/whatsapp/webhook - Debe rechazar con 403 cuando el token es incorrecto")
    void testVerificarWebhookTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "token_falso")
                        .param("hub.challenge", "1234567890"))
                .andExpect(status().isForbidden());
    }
}
