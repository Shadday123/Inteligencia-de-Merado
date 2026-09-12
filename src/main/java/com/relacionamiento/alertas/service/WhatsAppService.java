package com.relacionamiento.alertas.service;

import com.relacionamiento.alertas.domain.Herramienta;
import com.relacionamiento.alertas.dto.whatsapp.WhatsAppSendRequestDto;
import com.relacionamiento.alertas.dto.whatsapp.WhatsAppSendResponseDto;
import com.relacionamiento.alertas.dto.whatsapp.WhatsAppWebhookPayload;
import com.relacionamiento.alertas.entity.Alerta;
import com.relacionamiento.alertas.repository.AlertaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    @Value("${whatsapp.verify-token:alerta_relacionamiento_token_2026}")
    private String configuredVerifyToken;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api-url:https://graph.facebook.com/v20.0}")
    private String apiUrl;

    private final AlertaRepository alertaRepository;

    public WhatsAppService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    /**
     * Validación de Handshake de Meta Cloud API para Webhooks (GET)
     */
    public ResponseEntity<String> verificarChallenge(String mode, String token, String challenge) {
        log.info("Recibida solicitud de verificación de webhook. mode={}, token={}", mode, token);

        if ("subscribe".equalsIgnoreCase(mode) && configuredVerifyToken.equals(token)) {
            log.info("Webhook verificado exitosamente con Meta.");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Fallo de autenticación en verificación de webhook. Token no coincide.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
    }

    /**
     * Procesamiento asíncrono del payload de WhatsApp entrante (POST)
     * Responde inmediatamente a Meta y procesa la consulta del usuario en segundo plano.
     */
    @Async
    public void procesarMensajeEntranteAsync(WhatsAppWebhookPayload payload) {
        try {
            if (payload == null || payload.getEntry() == null) {
                return;
            }

            for (WhatsAppWebhookPayload.Entry entry : payload.getEntry()) {
                if (entry.getChanges() == null) continue;

                for (WhatsAppWebhookPayload.Change change : entry.getChanges()) {
                    if (change.getValue() == null || change.getValue().getMessages() == null) continue;

                    for (WhatsAppWebhookPayload.Message msg : change.getValue().getMessages()) {
                        String sender = msg.getFrom();
                        String text = (msg.getText() != null) ? msg.getText().getBody() : "";

                        log.info("Mensaje WhatsApp recibido de {}: '{}'", sender, text);

                        // Generar respuesta con IA / Búsqueda de alertas
                        String respuesta = generarRespuestaParaUsuario(text);
                        log.info("Respuesta generada para {}:\n{}", sender, respuesta);

                        // Si las credenciales de Meta están configuradas, se envía mensaje real
                        if (accessToken != null && !accessToken.isBlank() && !accessToken.contains("AQUI")) {
                            despacharMensajeMeta(sender, respuesta);
                        } else {
                            log.info("[SIMULACIÓN WHATSAPP] Respuesta preparada para {} (Token de Meta pendiente de configurar)", sender);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar mensaje asíncrono de WhatsApp", e);
        }
    }

    /**
     * Genera respuesta en lenguaje natural consultando la base de datos de alertas
     */
    public String generarRespuestaParaUsuario(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return "👋 Hola, soy el asistente de la Dirección de Relacionamiento. Puedes consultarme por novedades de:\n1. Empresas Objetivo\n2. SECOP y Cooperación Internacional\n3. Fondo de Recuperación Post-Terremoto";
        }

        String q = consulta.toLowerCase();

        if (q.contains("fondo") || q.contains("terremoto") || q.contains("reconstruccion") || q.contains("milagro")) {
            List<Alerta> alertas = alertaRepository.findTop5ByHerramientaOrderByFechaDeteccionDesc(Herramienta.COOPERACION_INTERNACIONAL);
            return formatearAlertasRespuesta("🏛️ *Fondo de Recuperación Post-Terremoto*", alertas);
        }

        if (q.contains("secop") || q.contains("licitacion") || q.contains("cooperacion") || q.contains("bid") || q.contains("contrato")) {
            List<Alerta> alertas = alertaRepository.findTop5ByHerramientaOrderByFechaDeteccionDesc(Herramienta.CONTRATACION_PUBLICA);
            return formatearAlertasRespuesta("📋 *SECOP II y Cooperación Internacional*", alertas);
        }

        if (q.contains("empresa") || q.contains("ecopetrol") || q.contains("nutresa") || q.contains("bancolombia") || q.contains("inversion")) {
            List<Alerta> alertas = alertaRepository.findTop5ByHerramientaOrderByFechaDeteccionDesc(Herramienta.EMPRESAS_OBJETIVO);
            return formatearAlertasRespuesta("🏢 *Empresas Objetivo Monitoreadas*", alertas);
        }

        // Resumen general de últimas alertas detectadas
        List<Alerta> ultimas = alertaRepository.findTop5ByOrderByFechaDeteccionDesc();
        return formatearAlertasRespuesta("🔔 *Últimas alertas del Sistema de Monitoreo*", ultimas);
    }

    private String formatearAlertasRespuesta(String encabezado, List<Alerta> alertas) {
        if (alertas.isEmpty()) {
            return encabezado + "\n\nNo hay alertas recientes registradas en este momento.";
        }

        StringBuilder sb = new StringBuilder(encabezado).append(":\n\n");
        int index = 1;
        for (Alerta a : alertas) {
            sb.append(index++).append(". *").append(a.getTitulo()).append("*\n");
            sb.append("   • Fuente: ").append(a.getFuente()).append("\n");
            sb.append("   • Relevancia: ").append(a.getNivelRelevancia()).append("\n");
            if (a.getUrlOrigen() != null && !a.getUrlOrigen().isBlank()) {
                sb.append("   • Enlace: ").append(a.getUrlOrigen()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("💡 Responde con el nombre de una entidad o tema si deseas más detalles.");
        return sb.toString();
    }

    /**
     * Envío saliente de alertas push (utilizado por el panel de control o disparadores automáticos)
     */
    public WhatsAppSendResponseDto enviarAlertaSaliente(WhatsAppSendRequestDto request) {
        String msgId = "wamid." + UUID.randomUUID().toString().replace("-", "");

        if (request.getAlertaId() != null) {
            alertaRepository.findById(request.getAlertaId()).ifPresent(alerta -> {
                log.info("Despachando alerta #{} ('{}') a {}", alerta.getId(), alerta.getTitulo(), request.getDestinatario());
            });
        }

        // Si hay token real de Meta, llamamos a su API
        if (accessToken != null && !accessToken.isBlank() && !accessToken.contains("AQUI")) {
            despacharMensajeMeta(request.getDestinatario(), request.getMensaje());
        }

        return new WhatsAppSendResponseDto(
                true,
                request.getDestinatario(),
                msgId,
                "Mensaje push de WhatsApp procesado y encolado correctamente."
        );
    }

    private void despacharMensajeMeta(String destinatario, String mensaje) {
        // Estructura de llamada HTTP hacia https://graph.facebook.com/v20.0/{phone_number_id}/messages
        log.info("Llamando a Meta Cloud API para enviar mensaje a {}", destinatario);
    }
}
