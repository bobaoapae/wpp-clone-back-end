package br.com.zapia.wppclone.modelo.dto;

public class UsuarioUpdateDTO {

    private String nome;
    @DTORelation
    private PermissaoDTO permissao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public PermissaoDTO getPermissao() {
        return permissao;
    }

    public void setPermissao(PermissaoDTO permissao) {
        this.permissao = permissao;
    }
}
