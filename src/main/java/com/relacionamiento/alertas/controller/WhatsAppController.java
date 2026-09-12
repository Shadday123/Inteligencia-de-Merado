package com.relacionamiento.alertas.controller;

import com.relacionamiento.alertas.dto.response.ApiResponseDto;
import com.relacionamiento.alertas.dto.whatsapp.WhatsAppSendRequestDto;
import com.relacionamiento.alertas.dto.whatsapp.WhatsAppSendResponseDto;
import com.relacionamiento.alertas.dto.whatsapp.WhatsAppWebhookPayload;
import com.relacionamiento.alertas.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "3. WhatsApp Business API & Webhook", description = "Integración con Meta Cloud API para verificación de Webhooks, recepción de mensajes y despacho de alertas push.")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/webhook")
    @Operation(summary = "Handshake de verificación de Webhook para Meta Cloud API (GET)",
            description = "Meta envía este request para verificar la propiedad del Webhook comparando el token secreto y devolviendo el challenge.")
    public ResponseEntity<String> verifyWebhook(
            @Parameter(name = "hub.mode", description = "Modo enviado por Meta (debe ser 'subscribe')")
            @RequestParam(name = "hub.mode") String mode,
            @Parameter(name = "hub.verify_token", description = "Token secreto configurado en el portal de Meta")
            @RequestParam(name = "hub.verify_token") String verifyToken,
            @Parameter(name = "hub.challenge", description = "Desafío aleatorio generado por Meta")
            @RequestParam(name = "hub.challenge") String challenge) {

        return whatsAppService.verificarChallenge(mode, verifyToken, challenge);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Recepción de eventos y mensajes entrantes de WhatsApp (POST)",
            description = "Recibe las notificaciones de Meta en tiempo real. Responde HTTP 200 inmediatamente y procesa la consulta de forma asíncrona.")
    public ResponseEntity<String> receiveWebhookEvent(@RequestBody WhatsAppWebhookPayload payload) {
        whatsAppService.procesarMensajeEntranteAsync(payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @PostMapping("/enviar-alerta")
    @Operation(summary = "Despacho manual de alerta push vía WhatsApp a un destinatario")
    public ResponseEntity<ApiResponseDto<WhatsAppSendResponseDto>> enviarAlerta(
            @Valid @RequestBody WhatsAppSendRequestDto request) {
        WhatsAppSendResponseDto res = whatsAppService.enviarAlertaSaliente(request);
        return ResponseEntity.ok(ApiResponseDto.ok("Alerta encolada para envío por WhatsApp", res));
    }
}
