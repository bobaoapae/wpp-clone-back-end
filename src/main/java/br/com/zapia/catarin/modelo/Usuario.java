package br.com.zapia.catarin.modelo;

import br.com.zapia.catarin.listenners.PasswordUsuariosListenner;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uuid")
@Entity
@EntityListeners(PasswordUsuariosListenner.class)
public class Usuario extends Entidade {

    @Column(nullable = false)
    private String nome;
    @Column(unique = true, nullable = false)
    private String login;
    @JsonIgnore
    @Column(nullable = false)
    private String senha;
    @JsonManagedReference
    @ManyToOne
    private Permissao permissao;
    @JsonIgnore
    @Transient
    private boolean updateSenha;

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
        setUpdateSenha(true);
        this.senha = senha;
    }

    public Permissao getPermissao() {
        return permissao;
    }

    public void setPermissao(Permissao permissao) {
        this.permissao = permissao;
    }

    public boolean isUpdateSenha() {
        return updateSenha;
    }

    public void setUpdateSenha(boolean updateSenha) {
        this.updateSenha = updateSenha;
    }
}
