package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.PinChat)
public class PinChatHandler extends HandlerWebSocket<String> {

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String payload) {
        return whatsAppClone.getWhatsAppClient().findChatById(payload).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                return chat.pinChat().thenApply(result -> {
                    return new WebSocketResponse(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value());
                });
            }
        });
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
