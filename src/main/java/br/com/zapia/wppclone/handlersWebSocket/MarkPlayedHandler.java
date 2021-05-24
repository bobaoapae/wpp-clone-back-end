package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.AudioMessage;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.MarkPlayed)
public class MarkPlayedHandler extends HandlerWebSocket<String> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String msgId) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findMessage(msgId).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                if (msg instanceof AudioMessage audioMessage) {
                    return audioMessage.markPlayed().thenApply(aVoid -> {
                        return new WebSocketResponse(HttpStatus.OK.value());
                    });
                } else {
                    return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST.value()));
                }
            }
        });
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
