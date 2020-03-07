package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.AddCustomPropertyRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "addCustomProperty")
public class AddCustomPropertyHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws Exception {
        AddCustomPropertyRequest addCustomPropertyRequest = new ObjectMapper().readValue((String) payload, AddCustomPropertyRequest.class);
        if (addCustomPropertyRequest.getType().equals(AddCustomPropertyRequest.Type.CHAT)) {
            return whatsAppClone.getDriver().getFunctions().getChatById(addCustomPropertyRequest.getId()).thenCompose(chat -> {
                return chat.addCustomProperty(addCustomPropertyRequest.getValue().getKey(), addCustomPropertyRequest.getValue().getValue());
            }).thenApply(jsValue -> {
                return new WebSocketResponse(HttpStatus.OK);
            });
        } else if (addCustomPropertyRequest.getType().equals(AddCustomPropertyRequest.Type.MSG)) {
            return whatsAppClone.getDriver().getFunctions().getMessageById(addCustomPropertyRequest.getId()).thenCompose(chat -> {
                return chat.addCustomProperty(addCustomPropertyRequest.getValue().getKey(), addCustomPropertyRequest.getValue().getValue());
            }).thenApply(jsValue -> {
                return new WebSocketResponse(HttpStatus.OK);
            });
        } else {
            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_IMPLEMENTED));
        }
    }
}
