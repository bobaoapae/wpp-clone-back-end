package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "sendPresenceUnavailable")
public class SendPresenceUnavailableHandler extends HandlerWebSocket<Void> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Void payload) {
        return whatsAppClone.getWhatsAppClient().sendPresenceUnavailable().thenApply(aVoid -> {
            return new WebSocketResponse(HttpStatus.OK.value());
        });
    }

    @Override
    public Class<Void> getClassType() {
        return Void.class;
    }
}
