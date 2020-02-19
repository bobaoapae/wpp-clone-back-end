package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "getAllQuickReplys")
public class GetAllQuickReplysHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        return whatsAppClone.getDriver().getFunctions().isBusiness().thenCompose(aBoolean -> {
            if (aBoolean) {
                return whatsAppClone.getSerializadorWhatsApp().serializarAllQuickReplys().thenApply(jsonNodes -> {
                    return new WebSocketResponse(HttpStatus.OK, jsonNodes);
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN));
            }
        });
    }
}
