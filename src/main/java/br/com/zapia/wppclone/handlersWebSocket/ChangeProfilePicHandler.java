package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "logout")
public class ChangeProfilePicHandler implements HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) {
        return whatsAppClone.getDriver().getFunctions().setProfilePicture((String) payload).thenApply(value -> {
            return new WebSocketResponse(value ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }
}
