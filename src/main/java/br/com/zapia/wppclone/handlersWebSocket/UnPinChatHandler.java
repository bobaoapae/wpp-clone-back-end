package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "unpinChat")
public class UnPinChatHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Object payload) {
        return whatsAppClone.getDriver().getFunctions().getChatById((String) payload).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return chat.setPin(false).thenApply(aVoid -> {
                    return new WebSocketResponse(HttpStatus.OK);
                });
            }
        });
    }
}
