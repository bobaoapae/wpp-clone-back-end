package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractGetGroupParticipants;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.GroupChat;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import br.com.zapia.wppclone.ws.WebSocketRequestSession;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
public class GetGroupParticipants extends AbstractGetGroupParticipants<WebSocketRequestSession> {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequest, String groupId) throws Exception {
        return whatsAppClone.getWhatsAppClient().findChatById(groupId).thenCompose(chat -> {
            if (chat instanceof GroupChat groupChat) {
                return groupChat.getAllParticipants().thenCompose(contacts -> {
                    return whatsAppClone.getWhatsAppSerializer().serializeParticipant(contacts).thenApply(jsonNodes -> {
                        return new WebSocketResponse(HttpStatus.OK_200, jsonNodes);
                    });
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST_400));
            }
        });
    }
}
