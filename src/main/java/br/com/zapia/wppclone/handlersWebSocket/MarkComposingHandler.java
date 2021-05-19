package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "markComposing")
public class MarkComposingHandler extends HandlerWebSocket<String> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String chatId) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findChatById(chatId).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                return chat.markComposing().thenApply(aVoid -> {
                    return new WebSocketResponse(HttpStatus.OK.value());
                });
            }
        });
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
