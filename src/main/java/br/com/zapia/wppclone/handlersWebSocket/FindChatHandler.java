package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "findChat")
public class FindChatHandler extends HandlerWebSocket<String> {

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String chatId) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findChatById(chatId).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                return whatsAppClone.getWhatsAppSerializer().serializeChat(chat).thenApply(jsonNodes -> {
                    return new WebSocketResponse(HttpStatus.OK.value(), jsonNodes);
                });
            }
        });
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
