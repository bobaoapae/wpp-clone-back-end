package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.ClearChatRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "clearChat")
public class ClearChatHandler extends HandlerWebSocket {
    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ClearChatRequest clearChatRequest = objectMapper.readValue((String) payload, ClearChatRequest.class);
        return whatsAppClone.getDriver().getFunctions().getChatById(clearChatRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else if (!usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR") || usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getOperadorPodeExcluirMsg()) {
                return chat.clearChat(clearChatRequest.isKeepFavorites()).thenApply(aVoid -> {
                    return new WebSocketResponse(HttpStatus.OK);
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN));
            }
        });
    }
}
