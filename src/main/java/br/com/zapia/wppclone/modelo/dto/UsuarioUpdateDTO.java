package br.com.zapia.wppclone.modelo.dto;

import javax.persistence.Id;
import java.util.UUID;

public class UsuarioUpdateDTO {

    @Id
    private UUID uuid;
    private String nome;
    @DTORelation
    private PermissaoDTO permissaoDTO;

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

    public PermissaoDTO getPermissaoDTO() {
        return permissaoDTO;
    }

    public void setPermissaoDTO(PermissaoDTO permissaoDTO) {
        this.permissaoDTO = permissaoDTO;
    }
}
