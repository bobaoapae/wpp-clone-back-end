package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "sendPresenceUnavailable")
public class SendPresenceUnavailableHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) {
        return whatsAppClone.getDriver().getFunctions().sendPresenceUnavailable().thenApply(aVoid -> {
            return new WebSocketResponse(HttpStatus.OK);
        });
    }
}
