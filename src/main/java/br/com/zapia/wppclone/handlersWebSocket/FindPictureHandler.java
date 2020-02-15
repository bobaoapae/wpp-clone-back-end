package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.servicos.DownloadFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "findPicture")
public class FindPictureHandler extends HandlerWebSocket {

    @Autowired
    private DownloadFileService downloadFileService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        return whatsAppClone.getDriver().getFunctions().getChatById((String) payload).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return chat.getContact().getThumb().thenApply(s -> {
                    if (!Strings.isNullOrEmpty(s)) {
                        try {
                            byte[] dearr = Base64.getDecoder().decode(s.split(",")[1]);
                            String extension = ".jpeg";
                            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
                            MimeType mime = allTypes.forName("image/jpeg");
                            String ex = mime.getExtension();
                            File f = File.createTempFile(chat.getId(), extension);
                            try (FileOutputStream fos = new FileOutputStream(f)) {
                                fos.write(dearr);
                            }
                            String key = downloadFileService.addFileToFutureDownload(f);
                            return new WebSocketResponse(HttpStatus.OK, key);
                        } catch (Exception e) {
                            return new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getStackTrace(e));
                        }
                    } else {
                        return new WebSocketResponse(HttpStatus.OK, "");
                    }
                });
            }
        });
    }
}
