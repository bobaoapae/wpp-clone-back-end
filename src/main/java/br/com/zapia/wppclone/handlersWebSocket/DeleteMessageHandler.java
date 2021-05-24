package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.DeleteMessageRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.DeleteMessage)
public class DeleteMessageHandler extends HandlerWebSocket<DeleteMessageRequest> {

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, DeleteMessageRequest deleteMessageRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findMessage(deleteMessageRequest.getMsgId()).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else if (!usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR") || usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getOperadorPodeExcluirMsg()) {
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

    @Override
    public Class<DeleteMessageRequest> getClassType() {
        return DeleteMessageRequest.class;
    }
}
