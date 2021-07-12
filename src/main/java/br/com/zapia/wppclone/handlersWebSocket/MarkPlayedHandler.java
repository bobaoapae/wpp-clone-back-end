package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractMarkPlayedHandler;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.AudioMessage;
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
public class MarkPlayedHandler extends AbstractMarkPlayedHandler<WebSocketRequestSession> {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, String msgId) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findMessage(msgId).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                if (msg instanceof AudioMessage audioMessage) {
                    return audioMessage.markPlayed().thenApply(aVoid -> {
                        return new WebSocketResponse(HttpStatus.OK.value());
                    });
                } else {
                    return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.BAD_REQUEST.value()));
                }
            }
        });
    }
}
