package br.com.zapia.wppclone.modelo.dto;

import javax.persistence.Id;
import java.util.UUID;

public class UsuarioOperadorUpdateDTO {

    @Id
    private UUID uuid;
    private String nome;

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
}
