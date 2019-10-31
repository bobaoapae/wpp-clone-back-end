package br.com.zapia.catarin.payloads;

import java.time.LocalDateTime;

public class LoginResponse {

    private String token;
    private LocalDateTime expireTime;
    private String tokenType;

    public LoginResponse() {
        this.tokenType = "bearer";
        this.expireTime = LocalDateTime.now().plusDays(7);
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
