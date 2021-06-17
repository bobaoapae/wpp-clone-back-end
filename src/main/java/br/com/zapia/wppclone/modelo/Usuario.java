package br.com.zapia.wppclone.modelo;

import br.com.zapia.wppclone.modelo.listenners.PasswordUsuariosListenner;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@EntityListeners(PasswordUsuariosListenner.class)
public class Usuario extends Entidade {

    @NotBlank
    @Column(nullable = false)
    private String nome;
    @NotBlank
    @Column(unique = true, nullable = false)
    private String login;
    @NotBlank
    @Column(nullable = false)
    private String telefone;
    @NotBlank
    @Column(nullable = false)
    private String senha;
    private int maxMemory;
    @ManyToOne(optional = false)
    private Permissao permissao;
    @NotNull
    @JoinColumn(name = "configuracao_uuid")
    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    private ConfiguracaoUsuario configuracao;
    @ManyToOne
    private Usuario usuarioPai;
    @OneToMany(mappedBy = "usuarioPai", cascade = CascadeType.ALL)
    private List<Usuario> usuariosFilhos;
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Cobranca> cobrancas;
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<TrocaDeNumero> trocasDeNumeros;
    @Transient
    private boolean updateSenha;

    public Usuario() {
        configuracao = new ConfiguracaoUsuario();
        configuracao.setUsuario(this);
    }

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

    public List<Cobranca> getCobrancas() {
        return cobrancas;
    }

    public void setCobrancas(List<Cobranca> cobrancas) {
        this.cobrancas = cobrancas;
    }

    public Permissao getPermissao() {
        return permissao;
    }

    public void setPermissao(Permissao permissao) {
        this.permissao = permissao;
    }

    public ConfiguracaoUsuario getConfiguracao() {
        return configuracao;
    }

    public void setConfiguracao(ConfiguracaoUsuario configuracao) {
        this.configuracao = configuracao;
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public List<TrocaDeNumero> getTrocasDeNumeros() {
        return trocasDeNumeros;
    }

    public void setTrocasDeNumeros(List<TrocaDeNumero> trocasDeNumeros) {
        this.trocasDeNumeros = trocasDeNumeros;
    }

    public Usuario getUsuarioResponsavelPelaInstancia() {
        if (getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
            return getUsuarioPai();
        } else {
            return this;
        }
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }
}
