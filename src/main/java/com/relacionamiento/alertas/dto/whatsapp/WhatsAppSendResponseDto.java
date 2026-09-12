package com.relacionamiento.alertas.dto.whatsapp;

import java.time.LocalDateTime;

public class WhatsAppSendResponseDto {
    private boolean enviado;
    private String destinatario;
    private String messageId;
    private String detalle;
    private LocalDateTime timestamp;

    public WhatsAppSendResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    public WhatsAppSendResponseDto(boolean enviado, String destinatario, String messageId, String detalle) {
        this.enviado = enviado;
        this.destinatario = destinatario;
        this.messageId = messageId;
        this.detalle = detalle;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isEnviado() { return enviado; }
    public void setEnviado(boolean enviado) { this.enviado = enviado; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
