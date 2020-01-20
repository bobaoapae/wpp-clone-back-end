package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "unpinChat")
public class UnPinChatHandler implements HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) {
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
