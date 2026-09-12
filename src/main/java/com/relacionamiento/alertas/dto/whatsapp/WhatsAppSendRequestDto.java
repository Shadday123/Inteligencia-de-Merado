package com.relacionamiento.alertas.dto.whatsapp;

import jakarta.validation.constraints.NotBlank;

public class WhatsAppSendRequestDto {

    @NotBlank(message = "El número telefónico de destino es obligatorio (ej: 573001234567)")
    private String destinatario;

    @NotBlank(message = "El mensaje a enviar es obligatorio")
    private String mensaje;

    private Long alertaId;

    public WhatsAppSendRequestDto() {}

    public WhatsAppSendRequestDto(String destinatario, String mensaje, Long alertaId) {
        this.destinatario = destinatario;
        this.mensaje = mensaje;
        this.alertaId = alertaId;
    }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Long getAlertaId() { return alertaId; }
    public void setAlertaId(Long alertaId) { this.alertaId = alertaId; }
}
