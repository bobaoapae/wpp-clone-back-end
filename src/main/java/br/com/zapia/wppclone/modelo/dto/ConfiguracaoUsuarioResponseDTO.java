package br.com.zapia.wppclone.modelo.dto;

import java.util.UUID;

public class ConfiguracaoUsuarioResponseDTO {

    private UUID uuid;
    private boolean enviarNomeOperadores;
    private boolean operadorPodeExcluirMsg;

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

    public boolean isOperadorPodeExcluirMsg() {
        return operadorPodeExcluirMsg;
    }

    public void setOperadorPodeExcluirMsg(boolean operadorPodeExcluirMsg) {
        this.operadorPodeExcluirMsg = operadorPodeExcluirMsg;
    }
}
