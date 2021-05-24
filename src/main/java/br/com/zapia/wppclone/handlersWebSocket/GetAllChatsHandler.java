package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.GetAllChats)
public class GetAllChatsHandler extends HandlerWebSocket<Void> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Void unused) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppSerializer().serializeAllChats().thenApply(jsonNodes -> {
            return new WebSocketResponse(HttpStatus.OK.value(), jsonNodes);
        });
    }

    @Override
    public Class<Void> getClassType() {
        return Void.class;
    }
}
