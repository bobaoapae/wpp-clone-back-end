package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import modelo.MediaMessage;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "markPlayed")
public class MarkPlayedHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        return whatsAppClone.getDriver().getFunctions().getMessageById((String) payload).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                if (msg instanceof MediaMessage) {
                    return ((MediaMessage) msg).markPlayed().thenApply(aVoid -> {
                        return new WebSocketResponse(HttpStatus.OK);
                    });
                } else {
                    return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST));
                }
            }
        });
    }
}
