package br.com.zapia.wppclone.ws;

import br.com.zapia.wpp.api.model.payloads.WebSocketRequest;
import br.com.zapia.wppclone.modelo.Usuario;

public class WebSocketRequestSession extends WebSocketRequest {

    private Usuario usuario;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
