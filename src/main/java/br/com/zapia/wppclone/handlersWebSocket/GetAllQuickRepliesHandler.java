package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractGetAllQuickRepliesHandler;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import br.com.zapia.wppclone.ws.WebSocketRequestSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
public class GetAllQuickRepliesHandler extends AbstractGetAllQuickRepliesHandler<WebSocketRequestSession> {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, Void unused) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().getSelfInfo().thenCompose(selfInfo -> {
            if (selfInfo.isBusiness()) {
                return whatsAppClone.getWhatsAppSerializer().serializeAllQuickReplies().thenApply(jsonNodes -> {
                    return new WebSocketResponse(HttpStatus.OK.value(), jsonNodes);
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN.value()));
            }
        });
    }
}
