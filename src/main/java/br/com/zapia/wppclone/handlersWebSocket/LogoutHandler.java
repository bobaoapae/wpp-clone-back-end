package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "logout")
public class LogoutHandler implements HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) {
        return whatsAppClone.logout().thenApply(aVoid -> {
            return new WebSocketResponse(HttpStatus.OK);
        });
    }
}
