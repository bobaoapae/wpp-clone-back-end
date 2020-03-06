package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.DeleteMessageRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "deleteMessage")
public class DeleteMessageHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        DeleteMessageRequest deleteMessageRequest = objectMapper.readValue((String) payload, DeleteMessageRequest.class);
        return whatsAppClone.getDriver().getFunctions().getMessageById(deleteMessageRequest.getMsgId()).thenCompose(msg -> {
            if (msg == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else if (!usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR") || usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getOperadorPodeExcluirMsg()) {
                if (deleteMessageRequest.isFromAll()) {
                    return msg.revokeMessage().thenApply(value -> {
                        return new WebSocketResponse(value ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
                    });
                } else {
                    return msg.deleteMessage().thenApply(value -> {
                        return new WebSocketResponse(value ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
                    });
                }
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN));
            }
        });
    }
}
