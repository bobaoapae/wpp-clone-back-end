package br.com.zapia.wppclone.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import javax.validation.constraints.NotNull;

@Entity
public class ConfiguracaoUsuario extends Entidade {

    @NotNull
    @OneToOne(mappedBy = "configuracao", optional = false)
    private Usuario usuario;
    @NotNull
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean enviarNomeOperadores;
    @NotNull
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean operadorPodeExcluirMsg;

    public ConfiguracaoUsuario() {
        enviarNomeOperadores = false;
        operadorPodeExcluirMsg = true;
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

    public Boolean getOperadorPodeExcluirMsg() {
        return operadorPodeExcluirMsg;
    }

    public void setOperadorPodeExcluirMsg(Boolean operadorPodeExcluirMsg) {
        this.operadorPodeExcluirMsg = operadorPodeExcluirMsg;
    }
}
