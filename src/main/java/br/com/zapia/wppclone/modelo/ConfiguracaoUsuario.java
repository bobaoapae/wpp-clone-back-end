package br.com.zapia.wppclone.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

@Entity
public class ConfiguracaoUsuario extends Entidade {

    @NotNull
    @OneToOne(mappedBy = "configuracao", optional = false)
    private Usuario usuario;
    @NotNull
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean enviarNomeOperadores;

    public ConfiguracaoUsuario() {
        enviarNomeOperadores = false;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Boolean getEnviarNomeOperadores() {
        return enviarNomeOperadores;
    }

    public void setEnviarNomeOperadores(Boolean enviarNomeOperadores) {
        this.enviarNomeOperadores = enviarNomeOperadores;
    }
}
