package br.com.zapia.wppclone.modelo.dto;

import jakarta.persistence.Id;

import java.util.UUID;

public class WhatsAppObjectWithIdPropertyUpdateDTO {
    @Id
    private UUID uuid;
    private String value;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
