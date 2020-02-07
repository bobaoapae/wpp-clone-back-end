package br.com.zapia.wppclone.modelo.dto;

import java.util.UUID;

public class ConfiguracaoUsuarioResponseDTO {

    private UUID uuid;
    private boolean enviarNomeOperadores;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isEnviarNomeOperadores() {
        return enviarNomeOperadores;
    }

    public void setEnviarNomeOperadores(boolean enviarNomeOperadores) {
        this.enviarNomeOperadores = enviarNomeOperadores;
    }
}
