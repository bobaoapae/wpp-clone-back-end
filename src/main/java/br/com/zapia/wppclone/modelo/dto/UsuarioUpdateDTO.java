package br.com.zapia.wppclone.modelo.dto;

import javax.persistence.Id;
import java.util.UUID;

public class UsuarioUpdateDTO {

    @Id
    private UUID uuid;
    private String nome;
    @DTORelation
    private UUID permissao;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public UUID getPermissao() {
        return permissao;
    }

    public void setPermissao(UUID permissao) {
        this.permissao = permissao;
    }
}
