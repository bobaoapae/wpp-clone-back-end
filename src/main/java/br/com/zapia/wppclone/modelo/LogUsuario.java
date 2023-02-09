package br.com.zapia.wppclone.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import javax.validation.constraints.NotBlank;

@Entity
public class LogUsuario extends Entidade {

    @ManyToOne
    private Usuario usuario;
    @NotBlank
    @Column(nullable = false)
    private String log;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
