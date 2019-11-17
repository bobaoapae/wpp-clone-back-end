package br.com.zapia.catarin.modelo.dto;

import java.util.UUID;

public class PermissaoResponseDTO {

    private UUID uuid;
    private String permissao;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getPermissao() {
        return permissao;
    }

    public void setPermissao(String permissao) {
        this.permissao = permissao;
    }
}
