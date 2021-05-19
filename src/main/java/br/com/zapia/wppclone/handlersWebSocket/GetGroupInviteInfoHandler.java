package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "getGroupInviteInfoHandler")
public class GetGroupInviteInfoHandler extends HandlerWebSocket<String> {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, String inviteCode) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findGroupInviteInfo(inviteCode).thenCompose(inviteInfo -> {
            if (inviteInfo == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                return whatsAppClone.getWhatsAppSerializer().serializeGroupInviteLinkInfo(inviteInfo).thenApply(jsonNodes -> {
                    return new WebSocketResponse(HttpStatus.OK.value(), jsonNodes);
                });
            }
        });
    }

    @Override
    public Class<String> getClassType() {
        return String.class;
    }
}
