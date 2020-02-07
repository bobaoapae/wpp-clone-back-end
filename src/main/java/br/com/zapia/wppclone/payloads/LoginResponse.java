package br.com.zapia.wppclone.payloads;

import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;

import java.time.LocalDateTime;

public class LoginResponse {

    private UsuarioResponseDTO usuario;
    private String token;
    private LocalDateTime expireTime;
    private String tokenType;

    public LoginResponse() {
        this.tokenType = "bearer";
        this.expireTime = LocalDateTime.now().plusDays(7);
    }

    public UsuarioResponseDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioResponseDTO usuario) {
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
