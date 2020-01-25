package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "sendPresenceUnavailable")
public class SendPresenceUnavailableHandler implements HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) {
        return whatsAppClone.getDriver().getFunctions().sendPresenceUnavailable().thenApply(aVoid -> {
            return new WebSocketResponse(HttpStatus.OK);
        });
    }
}
