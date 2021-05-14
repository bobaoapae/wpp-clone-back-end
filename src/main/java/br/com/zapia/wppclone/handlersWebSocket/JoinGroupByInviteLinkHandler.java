package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "joinGroupByInviteLinkHandler")
public class JoinGroupByInviteLinkHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        return whatsAppClone.getDriver().getFunctions().getGroupInviteLinkInfo((String) payload).thenCompose(inviteInfo -> {
            if (inviteInfo == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return inviteInfo.alreadyJoined().thenCompose(aBoolean -> {
                    if(aBoolean){
                        return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.PRECONDITION_FAILED, "Aleardy joined this group"));
                    }

                    return inviteInfo.joinGroup().thenApply(aBoolean1 -> {
                        if(aBoolean1)
                            return new WebSocketResponse(HttpStatus.OK);

                        return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
                });
            }
        });
    }
}
