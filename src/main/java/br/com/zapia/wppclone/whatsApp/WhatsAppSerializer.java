package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wpp.client.docker.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Scope("usuario")
@Service
public class WhatsAppSerializer {

    @Autowired
    @Lazy
    private WhatsAppClone whatsAppClone;
    private ObjectMapper objectMapper;
    private final Logger log = Logger.getLogger(WhatsAppSerializer.class.getName());


    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<ArrayNode> serializeAllQuickReplies() {
        return whatsAppClone.getWhatsAppClient().getAllQuickReplies().thenApply(quickReplies -> {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            quickReplies.forEach(quickReply -> arrayNode.add(quickReply.getJsonNode()));
            return arrayNode;
        });
    }

    @Async
    public CompletableFuture<ArrayNode> serializeAllChats() {
        return whatsAppClone.getWhatsAppClient().getAllChats().thenApply(chats -> {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            chats.forEach(chat -> arrayNode.add(chat.getJsonNode()));
            return arrayNode;
        });
    }

    @Async
    public CompletableFuture<ArrayNode> serializeAllContacts() {
        return whatsAppClone.getWhatsAppClient().getAllContacts().thenApply(contacts -> {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            contacts.forEach(contact -> arrayNode.add(contact.getJsonNode()));
            return arrayNode;
        });
    }

    @Async
    public CompletableFuture<ObjectNode> serializeContact(Contact contact) {
        var objectNode = Objects.requireNonNull(converterParaObjectNode(contact));
        return CompletableFuture.completedFuture(objectNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializeContact(List<Contact> contacts) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        contacts.forEach(contact -> arrayNode.add(contact.getJsonNode()));
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializeParticipant(GroupParticipant participant) {
        var objectNode = Objects.requireNonNull(converterParaObjectNode(participant));
        return CompletableFuture.completedFuture(objectNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializeParticipant(List<GroupParticipant> participants) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        participants.forEach(contact -> arrayNode.add(contact.getJsonNode()));
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializeChat(Chat chat) {
        var objectNode = Objects.requireNonNull(converterParaObjectNode(chat));
        return CompletableFuture.completedFuture(objectNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializeChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(serializeChat(chat).join());
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarChat", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializeMsg(Message message) {
        ObjectNode msgNode = Objects.requireNonNull(converterParaObjectNode(message));
        return CompletableFuture.completedFuture(msgNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializeMsg(List<Message> messages) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        messages.forEach(message -> {
            try {
                arrayNode.add(serializeMsg(message).join());
            } catch (Exception e) {
                log.log(Level.SEVERE, "SerializarMsg", e);
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    public CompletableFuture<ObjectNode> serializeGroupInviteLinkInfo(GroupInviteLinkInfo groupInviteLinkInfo) {
        ObjectNode groupInviteNode = Objects.requireNonNull(converterParaObjectNode(groupInviteLinkInfo));
        return CompletableFuture.completedFuture(groupInviteNode);
    }

    private ObjectNode converterParaObjectNode(WhatsAppObjectWithId whatsappObject) {
        return whatsappObject.getJsonNode().deepCopy();
    }
}
