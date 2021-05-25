package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "markUnRead")
public class MarkUnReadHandler extends HandlerWebSocket {

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws Exception {
        return whatsAppClone.getDriver().getFunctions().getChatById((String) payload).thenCompose(chat -> {
            return chat.markUnread().thenApply(unused -> {
                return new WebSocketResponse(HttpStatus.OK);
            });
        });
    }
}
