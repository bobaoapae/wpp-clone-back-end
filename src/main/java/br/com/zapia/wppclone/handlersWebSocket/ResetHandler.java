package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "reset")
public class ResetHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Object payload) {
        whatsAppClone.setForceShutdown(true);
        return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.OK));
    }
}
