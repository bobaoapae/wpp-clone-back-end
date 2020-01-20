package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "findChat")
public class FindChatHandler implements HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(WhatsAppClone whatsAppClone, Object payload) throws JsonProcessingException {
        return whatsAppClone.getDriver().getFunctions().getChatById((String) payload).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return whatsAppClone.getSerializadorWhatsApp().serializarChat(chat).thenApply(jsonNodes -> {
                    return new WebSocketResponse(HttpStatus.OK, jsonNodes);
                });
            }
        });
    }
}
