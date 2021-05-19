package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "loadEarly")
public class LoadEarlyHandler extends HandlerWebSocket<String> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String chatId) {
        return whatsAppClone.getWhatsAppClient().findChatById(chatId).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                return chat.loadEarly().thenApply(messages -> {
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
