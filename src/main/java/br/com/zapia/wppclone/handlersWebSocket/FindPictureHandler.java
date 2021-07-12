package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractFindPictureHandler;
import br.com.zapia.wpp.api.model.payloads.FindPictureRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.servicos.DownloadFileService;
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
public class FindPictureHandler extends AbstractFindPictureHandler<WebSocketRequestSession> {

    @Autowired
    private DownloadFileService downloadFileService;
    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, FindPictureRequest findPictureRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().getProfilePic(findPictureRequest.getId(), findPictureRequest.isFull()).thenApply(file -> {
            if (file != null) {
                String key = downloadFileService.addFileToFutureDownload(file);
                return new WebSocketResponse(HttpStatus.OK.value(), key);
            } else {
                return new WebSocketResponse(HttpStatus.OK.value(), "");
            }
        });
    }
}
