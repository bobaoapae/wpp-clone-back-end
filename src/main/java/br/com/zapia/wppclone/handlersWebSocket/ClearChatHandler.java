package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.ClearChatRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "clearChat")
public class ClearChatHandler extends HandlerWebSocket<ClearChatRequest> {

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, ClearChatRequest clearChatRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findChatById(clearChatRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else if (!usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR") || usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getOperadorPodeExcluirMsg()) {
                return chat.clearMessages(clearChatRequest.isKeepFavorites()).thenApply(aVoid -> {
                    return new WebSocketResponse(HttpStatus.OK.value());
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN.value()));
            }
        });
    }

    @Override
    public Class<ClearChatRequest> getClassType() {
        return ClearChatRequest.class;
    }
}
