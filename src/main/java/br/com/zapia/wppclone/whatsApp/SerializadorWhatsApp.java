package br.com.zapia.wppclone.whatsApp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import modelo.Chat;
import modelo.GroupJoinMetadata;
import modelo.Message;
import modelo.WhatsappObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    @Autowired
    @Lazy
    private WhatsAppClone whatsAppClone;
    private ObjectMapper objectMapper;
    private final Logger log = Logger.getLogger(SerializadorWhatsApp.class.getName());


    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<ArrayNode> serializarAllQuickReplys() {
        try {
            return CompletableFuture.completedFuture((ArrayNode) objectMapper.readTree(whatsAppClone.getDriver().convertToJson(whatsAppClone.getDriver().executeJavaScript("Array.from(Store.QuickReply.toJSON())"))));
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "SerializarAllQuickReplys", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<ArrayNode> serializarAllChats() {
        try {
            return CompletableFuture.completedFuture((ArrayNode) objectMapper.readTree(whatsAppClone.getDriver().convertToJson(whatsAppClone.getDriver().executeJavaScript("Store.Chat.toJSON()"))));
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "SerializarAllChats", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<ArrayNode> serializarAllContacts() {
        try {
            return CompletableFuture.completedFuture((ArrayNode) objectMapper.readTree(whatsAppClone.getDriver().convertToJson(whatsAppClone.getDriver().executeJavaScript("Store.Contact.filter(e=> e. isAddressBookContact || e.isWAContact && e.isMyContact).map(e=> e.toJSON())"))));
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "SerializarAllContacts", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) {
        return serializarChat(chat, false);
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat, boolean withPicture) {
        ObjectNode chatNode = Objects.requireNonNull(converterParaObjectNode(chat));
        if (withPicture) {
            return chat.getContact().getThumb().thenApply(s -> {
                chatNode.put("picture", s);
                return chatNode;
            }).exceptionally(throwable -> {
                chatNode.put("picture", "");
                return chatNode;
            });
        }
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(serializarChat(chat).join());
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
                arrayNode.add(serializarMsg(message).join());
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarMsg", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializarGroupInviteLinkInfo(GroupJoinMetadata inviteInfo) {
        ObjectNode msgNode = Objects.requireNonNull(converterParaObjectNode(inviteInfo));
        return CompletableFuture.completedFuture(msgNode);
    }

    private ObjectNode converterParaObjectNode(WhatsappObject whatsappObject) {
        try {
            return (ObjectNode) objectMapper.readTree(whatsappObject.toJson());
        } catch (IOException e) {
            log.log(Level.SEVERE, "ConverterParaObjectNode", e);
            return null;
        }
    }
}
