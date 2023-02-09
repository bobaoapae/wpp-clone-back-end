package br.com.zapia.wppclone.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import javax.validation.constraints.NotBlank;

@Entity
public class TrocaDeNumero extends Entidade {

    @ManyToOne
    private Usuario usuario;
    @NotBlank
    @Column(nullable = false)
    private String novoNumero;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNovoNumero() {
        return novoNumero;
    }

    public void setNovoNumero(String novoNumero) {
        this.novoNumero = novoNumero;
    }
}
