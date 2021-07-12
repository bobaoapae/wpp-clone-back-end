package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractDeleteMessageHandler;
import br.com.zapia.wpp.api.model.payloads.DeleteMessageRequest;
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
public class DeleteMessageHandler extends AbstractDeleteMessageHandler<WebSocketRequestSession> {

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, DeleteMessageRequest deleteMessageRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findMessage(deleteMessageRequest.getMsgId()).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else if (!webSocketRequestSession.getUsuario().getPermissao().getPermissao().equals("ROLE_OPERATOR") || webSocketRequestSession.getUsuario().getUsuarioResponsavelPelaInstancia().getConfiguracao().getOperadorPodeExcluirMsg()) {
                if (deleteMessageRequest.isFromAll()) {
                    return msg.revoke().thenApply(value -> {
                        return new WebSocketResponse(value ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
                } else {
                    return msg.delete().thenApply(value -> {
                        return new WebSocketResponse(value ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
                }
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN.value()));
            }
        });
    }
}
