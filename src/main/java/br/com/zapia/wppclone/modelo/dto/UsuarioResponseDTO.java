package br.com.zapia.wppclone.modelo.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.List;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uuid")
public class UsuarioResponseDTO {

    private UUID uuid;
    private String login;
    private String nome;
    private String telefone;
    private ConfiguracaoUsuarioResponseDTO configuracao;
    private boolean ativo;
    private PermissaoResponseDTO permissao;
    private List<UsuarioResponseDTO> usuariosFilhos;
    @JsonManagedReference
    private UsuarioResponseDTO usuarioPai;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public PermissaoResponseDTO getPermissao() {
        return permissao;
    }

    public void setPermissao(PermissaoResponseDTO permissao) {
        this.permissao = permissao;
    }

    public List<UsuarioResponseDTO> getUsuariosFilhos() {
        return usuariosFilhos;
    }

    public void setUsuariosFilhos(List<UsuarioResponseDTO> usuariosFilhos) {
        this.usuariosFilhos = usuariosFilhos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public ConfiguracaoUsuarioResponseDTO getConfiguracao() {
        return configuracao;
    }

    public void setConfiguracao(ConfiguracaoUsuarioResponseDTO configuracao) {
        this.configuracao = configuracao;
    }

    public UsuarioResponseDTO getUsuarioPai() {
        return usuarioPai;
    }

    public void setUsuarioPai(UsuarioResponseDTO usuarioPai) {
        this.usuarioPai = usuarioPai;
    }
}
