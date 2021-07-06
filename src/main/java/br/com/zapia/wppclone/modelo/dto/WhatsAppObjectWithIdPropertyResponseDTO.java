package br.com.zapia.wppclone.modelo.dto;

import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;

public class WhatsAppObjectWithIdPropertyResponseDTO extends WhatsAppObjectWithIdPropertyCreateDTO {

    private WhatsAppObjectWithIdType type;

    public WhatsAppObjectWithIdType getType() {
        return type;
    }

    public void setType(WhatsAppObjectWithIdType type) {
        this.type = type;
    }
}
