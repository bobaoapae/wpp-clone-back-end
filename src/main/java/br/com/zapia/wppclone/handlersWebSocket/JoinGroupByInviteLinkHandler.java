package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractJoinGroupByInviteLinkHandler;
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
public class JoinGroupByInviteLinkHandler extends AbstractJoinGroupByInviteLinkHandler<WebSocketRequestSession> {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, String inviteCode) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().joinGroup(inviteCode).thenApply(inviteInfo -> {
            if (inviteInfo) {
                return new WebSocketResponse(HttpStatus.OK.value());
            } else {
                return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        });
    }
}
