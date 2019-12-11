package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import driver.WebWhatsDriver;
import modelo.Chat;
import modelo.Message;
import modelo.WhatsappObject;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Scope("usuario")
@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    private Logger log = Logger.getLogger(SerializadorWhatsApp.class.getName());


    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<Void> updatePictureChat(WhatsAppClone whatsAppClone, WebSocketSession session, String chatId) {
        Chat chatById = whatsAppClone.getDriver().getFunctions().getChatById(chatId);
        ObjectNode chatNode = objectMapper.createObjectNode();
        chatNode.put("id", chatId);
        try {
            if (chatById != null) {
                chatNode.put("picture", chatById.getContact().getThumb());
            }
            whatsAppClone.enviarEventoWpp(WhatsAppClone.TipoEventoWpp.CHAT_PICTURE, objectMapper.writeValueAsString(chatNode), session);
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "SerializarAllChats", e);
            whatsAppClone.enviarEventoWpp(WhatsAppClone.TipoEventoWpp.ERROR, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarAllChats(WebWhatsDriver driver) {
        try {
            return CompletableFuture.completedFuture((ArrayNode) objectMapper.readTree(driver.getBrowser().executeJavaScriptAndReturnValue("Store.Chat.toJSON()").asArray().toJSONString()));
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "SerializarAllChats", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) {
        return CompletableFuture.completedFuture(Util.pegarResultadoFuture(serializarChat(chat, false)));
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat, boolean withPicture) {
        ObjectNode chatNode = Objects.requireNonNull(converterParaObjectNode(chat));
        if (withPicture) {
            chatNode.put("picture", chat.getContact().getThumb());
        }
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarChat(chat)));
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarChat", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializarMsg(Message message) {
        ObjectNode msgNode = Objects.requireNonNull(converterParaObjectNode(message));
        return CompletableFuture.completedFuture(msgNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarMsg(List<Message> messages) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        messages.forEach(message -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarMsg(message)));
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarMsg", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    private ObjectNode converterParaObjectNode(WhatsappObject whatsappObject) {
        try {
            return (ObjectNode) objectMapper.readTree(utils.Util.callFunction(whatsappObject.getJsObject(), "toJSON").asObject().toJSONString());
        } catch (IOException e) {
            log.log(Level.SEVERE, "ConverterParaObjectNode", e);
            return null;
        }
    }
}
