package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "joinGroupByInviteLinkHandler")
public class JoinGroupByInviteLinkHandler extends HandlerWebSocket<String> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String inviteCode) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().joinGroup(inviteCode).thenApply(inviteInfo -> {
            if (inviteInfo) {
                return new WebSocketResponse(HttpStatus.OK.value());
            } else {
                return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        });
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
