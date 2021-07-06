package br.com.zapia.wppclone.modelo;

import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;

@Entity
public class WhatsAppObjectWithIdProperty extends Entidade {

    @NotBlank
    @Column(nullable = false)
    private String whatsAppId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WhatsAppObjectWithIdType type;
    @NotBlank
    @Column(nullable = false)
    private String key;
    @NotBlank
    @Column(nullable = false)
    private String value;

    public String getWhatsAppId() {
        return whatsAppId;
    }

    public void setWhatsAppId(String whatsAppId) {
        this.whatsAppId = whatsAppId;
    }

    public WhatsAppObjectWithIdType getType() {
        return type;
    }

    public void setType(WhatsAppObjectWithIdType type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
