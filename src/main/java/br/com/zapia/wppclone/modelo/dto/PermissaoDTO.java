package br.com.zapia.wppclone.modelo.dto;

import javax.persistence.Id;
import java.util.UUID;

public class PermissaoDTO {

    @Id
    private UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
