package br.com.zapia.catarin.modelo.dto;

public class UsuarioCreateDTO {

    private String login;
    private String senha;
    private String nome;
    @DTORelation
    private PermissaoDTO permissao;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

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
