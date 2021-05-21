package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.payloads.FindPictureRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.DownloadFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "findPicture")
public class FindPictureHandler extends HandlerWebSocket<FindPictureRequest> {

    @Autowired
    private DownloadFileService downloadFileService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, FindPictureRequest findPictureRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().getProfilePic(findPictureRequest.getId(), findPictureRequest.isFull()).thenApply(file -> {
            if (file != null) {
                String key = downloadFileService.addFileToFutureDownload(file);
                return new WebSocketResponse(HttpStatus.OK.value(), key);
            } else {
                return new WebSocketResponse(HttpStatus.OK.value(), "");
            }
        });
    }

    @Override
    public Class<FindPictureRequest> getClassType() {
        return FindPictureRequest.class;
    }
}
