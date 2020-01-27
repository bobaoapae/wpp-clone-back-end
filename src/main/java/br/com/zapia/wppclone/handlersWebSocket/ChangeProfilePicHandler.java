package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "logout")
public class ChangeProfilePicHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Object payload) {
        return whatsAppClone.getDriver().getFunctions().setProfilePicture((String) payload).thenApply(value -> {
            return new WebSocketResponse(value ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }
}
