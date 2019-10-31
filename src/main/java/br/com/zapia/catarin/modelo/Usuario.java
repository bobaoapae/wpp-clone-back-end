package br.com.zapia.catarin.modelo;

import br.com.zapia.catarin.listenners.PasswordUsuariosListenner;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ManyToOne;

@Entity
@EntityListeners(PasswordUsuariosListenner.class)
public class Usuario extends Entidade {

    @Column(nullable = false)
    private String nome;
    @Column(unique = true, nullable = false)
    private String login;
    @Column(nullable = false)
    private String senha;
    @ManyToOne
    private Permissao permissao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

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

    public Permissao getPermissao() {
        return permissao;
    }

    public void setPermissao(Permissao permissao) {
        this.permissao = permissao;
    }
}
