package br.com.zapia.wppclone.handlersWebSocket;

import br.com.zapia.wpp.api.model.handlersWebSocket.AbstractSendMessageHandler;
import br.com.zapia.wpp.api.model.payloads.SendMessageRequest;
import br.com.zapia.wpp.api.model.payloads.WebSocketResponse;
import br.com.zapia.wpp.client.docker.model.GroupChat;
import br.com.zapia.wpp.client.docker.model.Message;
import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty;
import br.com.zapia.wppclone.modelo.dto.WhatsAppObjectWithIdPropertyResponseDTO;
import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;
import br.com.zapia.wppclone.servicos.UploadFileService;
import br.com.zapia.wppclone.servicos.WhatsAppObjectWithPropertyService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import br.com.zapia.wppclone.ws.WebSocketRequestSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Component
@Scope("usuario")
public class SendMessageHandler extends AbstractSendMessageHandler<WebSocketRequestSession> {

    private static final Logger logger = Logger.getLogger(SendMessageHandler.class.getName());

    @Autowired
    @Lazy
    protected WhatsAppClone whatsAppClone;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private WhatsAppObjectWithPropertyService whatsAppObjectWithPropertyService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CompletableFuture<WebSocketResponse> handle(WebSocketRequestSession webSocketRequestSession, SendMessageRequest sendMessageRequest) throws JsonProcessingException {
        var usuario = webSocketRequestSession.getUsuario();
        return whatsAppClone.getWhatsAppClient().findChatById(sendMessageRequest.getChatId()).thenCompose(chat -> {
            if (chat == null) {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_FOUND.value()));
            } else {

                var isSendOperatorNameEnabled = usuario.getUsuarioResponsavelPelaInstancia().getConfiguracao().getEnviarNomeOperadores() && usuario.getPermissao().getPermissao().equals("ROLE_OPERATOR");
                var textMsg = sendMessageRequest.getText();
                if (isSendOperatorNameEnabled) {
                    Message lastMessage = chat.getLastMsg();
                    var lastUserSendMessage = lastMessage == null ? null : whatsAppObjectWithPropertyService.buscarPropriedade(WhatsAppObjectWithIdType.MESSAGE, lastMessage.getId(), "userSendMessage");
                    if (lastMessage == null || lastUserSendMessage == null || !usuario.getUuid().equals(UUID.fromString(lastUserSendMessage.getValue()))) {
                        if (sendMessageRequest.getFile() == null) {
                            textMsg = "*".concat(usuario.getNome()).concat(" diz:* ".concat(sendMessageRequest.getText()));
                        } else {
                            textMsg = "* Enviado por: ".concat(usuario.getNome()).concat("*");
                            if (sendMessageRequest.getText() != null) {
                                textMsg = textMsg.concat(" \n").concat(sendMessageRequest.getText());
                            }
                        }
                    }
                }


                var msgBuilder = new SendMessageRequest.Builder(chat.getId());

                msgBuilder.withText(textMsg);

                if (!Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                    msgBuilder.withQuotedMsg(sendMessageRequest.getQuotedMsg());
                }

                if (sendMessageRequest.getFile() != null) {
                    var file = uploadFileService.getAndRemoveFileUploaded(sendMessageRequest.getFile().getUuid());
                    if (file != null) {
                        var uuidUpload = whatsAppClone.getWhatsAppClient().uploadFile(file.getOriginalName(), file.getTempFile()).join();
                        msgBuilder.withFile(uuidUpload, fileBuilder -> fileBuilder.withCaption(sendMessageRequest.getText()).withForceDocument(sendMessageRequest.getFile().isForceDocument()));
                    }
                }

                if (sendMessageRequest.getLocation() != null) {
                    msgBuilder.withLocation(sendMessageRequest.getLocation().getLat(), sendMessageRequest.getLocation().getLng(), locationBuilder -> locationBuilder.withName(sendMessageRequest.getLocation().getName()));
                }

                if (chat instanceof GroupChat && sendMessageRequest.getMentionedContacts() != null) {
                    for (String mentionedContact : sendMessageRequest.getMentionedContacts()) {
                        msgBuilder.withMentionToContact(mentionedContact);
                    }
                }

                if (sendMessageRequest.getvCard() != null) {
                    msgBuilder.withVCard(sendMessageRequest.getvCard().getName(), sendMessageRequest.getvCard().getTelephone());
                }

                if (sendMessageRequest.getWebSite() != null) {
                    msgBuilder.withWebSite(sendMessageRequest.getWebSite());
                }

                if (sendMessageRequest.getButtons() != null) {
                    msgBuilder.withButtons(sendMessageRequest.getButtons().getTitle(), sendMessageRequest.getButtons().getFooter(), buttonsBuilder -> {
                        Arrays.stream(sendMessageRequest.getButtons().getButtons()).forEach(buttonsBuilder::withButton);
                    });
                }

                if (sendMessageRequest.getWhatsAppList() != null) {
                    msgBuilder.withList(listBuilder -> {
                        listBuilder
                                .withTitle(sendMessageRequest.getWhatsAppList().getTitle())
                                .withDescription(sendMessageRequest.getWhatsAppList().getDescription())
                                .withFooter(sendMessageRequest.getWhatsAppList().getFooter())
                                .withButtonText(sendMessageRequest.getWhatsAppList().getButtonText());
                        for (SendMessageRequest.Section section : sendMessageRequest.getWhatsAppList().getSections()) {
                            listBuilder.withSection(section.getTitle(), sectionBuilder -> {
                                for (SendMessageRequest.SectionItem row : section.getRows()) {
                                    sectionBuilder.withRow(row.getTitle(), row.getDescription()).build();
                                }
                            });
                        }
                    });
                }

                return chat.sendMessage(msgBuilder.build()).thenApply(message -> {
                    var userSendMessage = new WhatsAppObjectWithIdProperty();
                    userSendMessage.setType(WhatsAppObjectWithIdType.MESSAGE);
                    userSendMessage.setWhatsAppId(message.getId());
                    userSendMessage.setKey("userSendMessage");
                    userSendMessage.setValue(usuario.getUuid().toString());
                    if (whatsAppObjectWithPropertyService.salvar(userSendMessage)) {
                        whatsAppClone.enviarEventoWpp(WhatsAppClone.TypeEventWebSocket.CHANGE_PROPERTY_MESSAGE, modelMapper.map(userSendMessage, WhatsAppObjectWithIdPropertyResponseDTO.class));
                    }
                    return new WebSocketResponse(org.eclipse.jetty.http.HttpStatus.OK_200, whatsAppClone.getWhatsAppSerializer().serializeMsg(message));
                });
            }
        });
    }
}
