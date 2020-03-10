package br.com.zapia.wppclone.modelo.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
public class UsuarioBasicResponseDTO {

    private UUID uuid;
    private String nome;
    private String login;
    private String telefone;
    private PermissaoResponseDTO permissao;
    private ConfiguracaoUsuarioResponseDTO configuracao;
    private boolean ativo;
    @JsonManagedReference
    private UsuarioBasicResponseDTO usuarioPai;

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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public PermissaoResponseDTO getPermissao() {
        return permissao;
    }

    public void setPermissao(PermissaoResponseDTO permissao) {
        this.permissao = permissao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public UsuarioBasicResponseDTO getUsuarioPai() {
        return usuarioPai;
    }

    public void setUsuarioPai(UsuarioBasicResponseDTO usuarioPai) {
        this.usuarioPai = usuarioPai;
    }

    public ConfiguracaoUsuarioResponseDTO getConfiguracao() {
        return configuracao;
    }

    public void setConfiguracao(ConfiguracaoUsuarioResponseDTO configuracao) {
        this.configuracao = configuracao;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
