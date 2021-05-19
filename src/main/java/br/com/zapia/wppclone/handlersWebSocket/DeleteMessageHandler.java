package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.DeleteMessageRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "deleteMessage")
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
