package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractClearChatHandler;
import br.com.zapia.wpp.api.model.payloads.ClearChatRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.servicos.LogUsuarioService;
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
public class ClearChatHandler extends AbstractClearChatHandler<WebSocketRequestSession> {

    @Lazy
    @Autowired
    private WhatsAppClone whatsAppClone;
    @Autowired
    @Lazy
    private LogUsuarioService logUsuarioService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, ClearChatRequest clearChatRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findChatById(clearChatRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else if (!webSocketRequestSession.getUsuario().getPermissao().getPermissao().equals("ROLE_OPERATOR") || webSocketRequestSession.getUsuario().getUsuarioResponsavelPelaInstancia().getConfiguracao().getOperadorPodeExcluirMsg()) {
                logUsuarioService.registrarLog(webSocketRequestSession.getUsuario(), "Limpou a conversa %s".formatted(clearChatRequest.getChatId()));
                return chat.clearMessages(clearChatRequest.isKeepFavorites()).thenApply(aVoid -> {
                    return new WebSocketResponse(HttpStatus.OK.value());
                });
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.FORBIDDEN.value()));
            }
        });
    }
}
