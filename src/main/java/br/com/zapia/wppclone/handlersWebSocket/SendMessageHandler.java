package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.EventWebSocket;
import br.com.zapia.wpp.api.model.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wpp.api.model.payloads.SendMessageRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.Message;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.UploadFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Component
@Scope("usuario")
@HandlerWebSocketEvent(event = EventWebSocket.SendMessage)
public class SendMessageHandler extends HandlerWebSocket<SendMessageRequest> {

    @Autowired
    private UploadFileService uploadFileService;

    @Override
    public CompletableFuture<WebSocketResponse> handle(Usuario usuario, SendMessageRequest sendMessageRequest) throws JsonProcessingException {
        return whatsAppClone.getWhatsAppClient().findChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {
                Message lastMessage = chat.getLastMsg();
                //boolean flagAppend = lastMessage == null || !usuario.getUuid().toString().equals(lastMessage.getCustomProperty("usuario").join());
                boolean flagAppend = false;
                String textMsg;
                if (flagAppend && usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                    textMsg = "*".concat(usuario.getNome()).concat(" diz:* ").concat(sendMessageRequest.getMessage());
                } else {
                    textMsg = sendMessageRequest.getMessage();
                }
                String captionImage;
                if (flagAppend && usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR")) {
                    captionImage = "*Enviado por: ".concat(usuario.getNome()).concat("*");
                } else {
                    captionImage = sendMessageRequest.getMessage();
                }
                if (Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                    if (Strings.isNullOrEmpty(sendMessageRequest.getFileUUID())) {
                        return chat.sendMessage(textMsg).thenApply(message -> {
                            /*return message.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                return new WebSocketResponse(HttpStatus.OK, message.toJson());
                            });*/
                            return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(message).join());
                        });
                    } else {
                        File file = uploadFileService.getFileUploaded(sendMessageRequest.getFileUUID());
                        String fileName = file.getName().split("#")[0];
                        if (sendMessageRequest.getMessage().equalsIgnoreCase("sticker")) {
                            fileName += ".webp";
                        }
                        return chat.sendMessage(file, fileName, captionImage).thenApply(mediaMessage -> {
                            uploadFileService.removeFileUploaded(sendMessageRequest.getFileUUID());
                            /*return mediaMessage.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                return new WebSocketResponse(HttpStatus.OK, mediaMessage.toJson());
                            });*/
                            return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(mediaMessage).join());

                        });
                    }
                } else {
                    return whatsAppClone.getWhatsAppClient().findMessage(sendMessageRequest.getQuotedMsg()).thenCompose(message -> {
                        if (message != null) {
                            if (Strings.isNullOrEmpty(sendMessageRequest.getFileUUID())) {
                                return message.reply(textMsg).thenApply(message1 -> {
                                    /*return message1.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                        return new WebSocketResponse(HttpStatus.OK, message1.toJson());
                                    });*/
                                    return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(message1).join());
                                });
                            } else {
                                File file = uploadFileService.getFileUploaded(sendMessageRequest.getFileUUID());
                                return message.reply(file, file.getName().split("#")[0], captionImage).thenApply(mediaMessage -> {
                                    uploadFileService.removeFileUploaded(sendMessageRequest.getFileUUID());
                                    /*return mediaMessage.addCustomProperty("usuario", usuario.getUuid().toString()).thenApply(jsValue -> {
                                        return new WebSocketResponse(HttpStatus.OK, mediaMessage.toJson());
                                    });*/
                                    return new WebSocketResponse(HttpStatus.OK.value(), whatsAppClone.getWhatsAppSerializer().serializeMsg(mediaMessage).join());
                                });
                            }
                        } else {
                            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value(), "Quoted Message"));
                        }
                    });
                }
            }
        });
    }

    @Override
    public Class<SendMessageRequest> getClassType() {
        return SendMessageRequest.class;
    }
}
