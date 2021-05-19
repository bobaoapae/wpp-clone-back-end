package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "getAllContacts")
public class GetAllContactsHandler extends HandlerWebSocket<Void> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Void unused) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppSerializer().serializeAllContacts().thenApply(jsonNodes -> {
            return new WebSocketResponse(HttpStatus.OK.value(), jsonNodes);
        });
    }

    @Override
    public Class<Void> getClassType() {
        return Void.class;
    }
}
