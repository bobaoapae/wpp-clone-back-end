package br.com.zapia.wppclone.modelo.dto;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Permissao_;

import javax.persistence.Id;
import java.util.UUID;

public class UsuarioUpdateDTO {

    @Id
    private UUID uuid;
    private String nome;
    @DTORelation(classEntidade = Permissao.class, key = Permissao_.PERMISSAO)
    private String permissao;
    private int maxMemory;

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

    public String getPermissao() {
        return permissao;
    }

    public void setPermissao(String permissao) {
        this.permissao = permissao;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }
}
