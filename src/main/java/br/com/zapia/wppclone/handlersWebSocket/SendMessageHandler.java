package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wppclone.modelo.Usuario;
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
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, Object payload) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SendMessageRequest sendMessageRequest = objectMapper.readValue((String) payload, SendMessageRequest.class);
        return whatsAppClone.getDriver().getFunctions().getChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND));
            } else {
                return chat.getLastMsg().thenCompose(lastMessage -> {
                    boolean flagAppend = lastMessage == null || !usuario.getUuid().toString().equals(lastMessage.getCustomProperty("usuario").join());
                    String textMsg;
                    if (flagAppend && usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                        textMsg = "*".concat(usuario.getNome()).concat(" diz:* ").concat(sendMessageRequest.getMessage());
                    } else {
                        textMsg = sendMessageRequest.getMessage();
                    }
                    String captionImage;
                    if (flagAppend && usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                        textMsg = "*Enviado por: ".concat(usuario.getNome()).concat("*");
                    } else {
                        textMsg = sendMessageRequest.getMessage();
                    }


                    var msgBuilder = new MessageOptions.Builder();

                    msgBuilder.withText(textMsg);

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

                    return chat.sendMessage(msgBuilder.build()).thenCompose(message -> {
                        return message.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                            return new WebSocketResponse(org.eclipse.jetty.http.HttpStatus.OK_200, message.toJson());
                        });
                    });
                });
            }
        });
    }
}
