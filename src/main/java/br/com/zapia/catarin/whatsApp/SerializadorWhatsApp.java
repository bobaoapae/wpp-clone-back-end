package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import modelo.Chat;
import modelo.MediaMessage;
import modelo.Message;
import modelo.WhatsappObjectWithId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    private LoadingCache<WhatsappObjectWithId, ObjectNode> cache;
    @Lazy
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        cache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(WhatsappObjectWithId whatsappObject) throws IOException {
                                return (ObjectNode) objectMapper.readTree(whatsappObject.toJson());
                            }
                        });
    }

    public ObjectNode readTree(WhatsappObjectWithId whatsappObject) {
        return cache.getUnchecked(whatsappObject);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) throws IOException {
        ObjectNode chatNode = readTree(chat);
        ArrayNode arrayNode = objectMapper.createArrayNode();
        List<Message> allMessages = chat.getAllMessages();
        Collection<List<Message>> partition = Util.partition(allMessages, 2);
        List<CompletableFuture<ArrayNode>> futures = new ArrayList<>();
        partition.forEach(messages -> {
            futures.add(serializadorWhatsApp.serializarMsg(messages));
        });
        Util.pegarResultadosFutures(futures).forEach(arrayNode::addAll);
        chatNode.set("msgs", arrayNode);
        chatNode.putObject("contact").setAll(readTree(chat.getContact()));
        chatNode.put("picture", chat.getContact().getThumb());
        chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
        chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ArrayNode> serializarChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarChat(chat)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ObjectNode> serializarMsg(Message message) throws IOException {
        ObjectNode msgNode = readTree(message);
        if (message.getSender() != null) {
            msgNode.putObject("sender").setAll(readTree(message.getSender()));
        }
        if (message instanceof MediaMessage) {
            msgNode.put("filename", ((MediaMessage) message).getFileName());
            if (message.getJsObject().hasProperty("pageCount") && message.getJsObject().getProperty("pageCount").isNumber()) {
                msgNode.put("pageCount", message.getJsObject().getProperty("pageCount").asNumber().getValue());
            }
        }
        return CompletableFuture.completedFuture(msgNode);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ArrayNode> serializarMsg(List<Message> messages) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        messages.forEach(message -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarMsg(message)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }
}
