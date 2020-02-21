package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "loadEarly")
public class LoadEarlyHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) {
        return whatsAppClone.getDriver().getFunctions().getChatById((String) payload).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return chat.loadEarlierMsgs().thenCompose(messages -> {
                    if (messages != null) {
                        return whatsAppClone.getSerializadorWhatsApp().serializarMsg(messages).thenApply(jsonNodes -> {
                            return new WebSocketResponse(HttpStatus.OK, jsonNodes);
                        });
                    } else {
                        return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.OK));
                    }
                });
            }
        });
    }
}
