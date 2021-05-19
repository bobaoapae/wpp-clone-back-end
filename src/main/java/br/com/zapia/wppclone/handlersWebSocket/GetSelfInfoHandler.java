package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "getSelfInfo")
public class GetSelfInfoHandler extends HandlerWebSocket<Void> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Void o) throws Exception {
        return whatsAppClone.getWhatsAppClient().getSelfInfo().thenApply(selfInfo -> {
            return new WebSocketResponse(HttpStatus.OK.value(), selfInfo.getJsonNode());
        });
    }

    @Override
    public Class<Void> getClassType() {
        return Void.class;
    }
}
