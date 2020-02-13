package br.com.zapia.wppclone.modelo.dto;

import java.util.UUID;

public class TrocaDeNumeroDTO {

    private UUID uuid;
    private UsuarioBasicResponseDTO usuario;
    private String novoNumero;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UsuarioBasicResponseDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioBasicResponseDTO usuario) {
        this.usuario = usuario;
    }

    public String getNovoNumero() {
        return novoNumero;
    }

    public void setNovoNumero(String novoNumero) {
        this.novoNumero = novoNumero;
    }
}
