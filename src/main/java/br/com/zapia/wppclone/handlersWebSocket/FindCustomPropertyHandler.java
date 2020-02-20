package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.FindCustomPropertyRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdev.jxbrowser.chromium.JSObject;
import modelo.WhatsappObjectWithId;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "findCustomProperty")
public class FindCustomPropertyHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        FindCustomPropertyRequest findCustomPropertyRequest = objectMapper.readValue(payload.toString(), FindCustomPropertyRequest.class);
        CompletableFuture<JSObject> customPropertyFuture = null;
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {
        };
        switch (findCustomPropertyRequest.getType()) {
            case CHAT:
                customPropertyFuture = whatsAppClone.getDriver().getFunctions().getChatById(findCustomPropertyRequest.getId()).thenCompose(WhatsappObjectWithId::getAllCustomProperty);
                break;
            case MSG:
                customPropertyFuture = whatsAppClone.getDriver().getFunctions().getMessageById(findCustomPropertyRequest.getId()).thenCompose(WhatsappObjectWithId::getAllCustomProperty);
                break;
        }
        if (customPropertyFuture != null) {
            return customPropertyFuture.thenApply(jsObject -> {
                try {
                    return objectMapper.readValue(jsObject.toJSONString(), typeRef);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(o -> {
                return new WebSocketResponse(HttpStatus.OK, o);
            });
        } else {
            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
        }
    }
}
