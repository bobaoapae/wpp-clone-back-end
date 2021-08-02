package br.com.zapia.wppclone.modelo.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class OperatorCreateDTO {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Only alphanumeric characters are allowed")
    private String login;
    @NotBlank
    private String senha;
    @NotBlank
    private String nome;

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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
