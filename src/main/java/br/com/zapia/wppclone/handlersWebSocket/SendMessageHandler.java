package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.payloads.SendMessageRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.servicos.UploadFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import modelo.MessageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CompletableFuture;

@HandlerWebSocketEvent(event = "sendMessage")
public class SendMessageHandler extends HandlerWebSocket {

    @Autowired
    private UploadFileService uploadFileService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SendMessageRequest sendMessageRequest = objectMapper.readValue((String) payload, SendMessageRequest.class);
        return whatsAppClone.getDriver().getFunctions().getChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                var msgBuilder = new MessageOptions.Builder();

                msgBuilder.withText(sendMessageRequest.getMessage());

                if (!Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                    var quotedMsg = whatsAppClone.getDriver().getFunctions().getMessageById(sendMessageRequest.getQuotedMsg()).join();
                    if (quotedMsg != null) {
                        msgBuilder.withQuotedMsg(quotedMsg);
                    } else {
                        return CompletableFuture.completedFuture(new WebSocketResponse(org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404, "Quoted Message"));
                    }
                }

                if (!Strings.isNullOrEmpty(sendMessageRequest.getFileUUID())) {
                    var file = uploadFileService.getAndRemoveFileUploaded(sendMessageRequest.getFileUUID());
                    if (file != null) {
                        msgBuilder.withFile(file, fileBuilder -> fileBuilder.withName(file.getName().split("#")[0]));
                    }
                }

                return chat.sendMessage(msgBuilder.build()).thenApply(message -> {
                    return new WebSocketResponse(org.eclipse.jetty.http.HttpStatus.OK_200, message.toJson());
                });
            }
        });
    }
}
