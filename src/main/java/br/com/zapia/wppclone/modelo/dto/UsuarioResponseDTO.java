package br.com.zapia.wppclone.modelo.dto;

import java.util.UUID;

public class UsuarioResponseDTO {

    private UUID uuid;
    private String login;
    private String nome;
    private PermissaoResponseDTO permissao;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public PermissaoResponseDTO getPermissao() {
        return permissao;
    }

    public void setPermissao(PermissaoResponseDTO permissao) {
        this.permissao = permissao;
    }
}
