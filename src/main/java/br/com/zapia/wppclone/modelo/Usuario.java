package br.com.zapia.wppclone.modelo;

import br.com.zapia.wppclone.listenners.PasswordUsuariosListenner;

import javax.persistence.*;
import java.util.List;

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
    @ManyToOne
    private Usuario usuarioPai;
    @OneToMany(mappedBy = "usuarioPai", cascade = CascadeType.ALL)
    private List<Usuario> usuariosFilhos;
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

    public Usuario getUsuarioPai() {
        return usuarioPai;
    }

    public void setUsuarioPai(Usuario usuarioPai) {
        this.usuarioPai = usuarioPai;
    }

    public List<Usuario> getUsuariosFilhos() {
        return usuariosFilhos;
    }

    public void setUsuariosFilhos(List<Usuario> usuariosFilhos) {
        this.usuariosFilhos = usuariosFilhos;
    }

    public boolean isUpdateSenha() {
        return updateSenha;
    }

    public void setUpdateSenha(boolean updateSenha) {
        this.updateSenha = updateSenha;
    }
}
