package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "getAllContacts")
public class GetAllContactsHandler extends HandlerWebSocket {

    @Override
    public CompletableFuture<WebSocketResponse> handle(Object payload) throws JsonProcessingException {
        return whatsAppClone.getSerializadorWhatsApp().serializarAllContacts().thenApply(jsonNodes -> {
            return new WebSocketResponse(HttpStatus.OK, jsonNodes);
        });
    }
}
