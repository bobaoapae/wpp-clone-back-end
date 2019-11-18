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
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Scope("usuario")
@Service
public class SerializadorWhatsApp {

    private ObjectMapper objectMapper;
    private LoadingCache<WhatsappObjectWithId, ObjectNode> cache;
    private LoadingCache<Chat, ObjectNode> cacheChats;
    private LoadingCache<Message, ObjectNode> cacheMsgs;
    @Lazy
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        cache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .refreshAfterWrite(Duration.ofDays(1))
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(WhatsappObjectWithId whatsappObject) throws IOException {
                                return (ObjectNode) objectMapper.readTree(whatsappObject.toJson());
                            }
                        });
        cacheChats = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .refreshAfterWrite(Duration.ofHours(10))
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(Chat chat) throws ExecutionException {
                                ObjectNode chatNode;
                                try {
                                    chatNode = cache.get(chat);
                                } catch (IllegalStateException e) {
                                    cache.refresh(chat);
                                    chatNode = cache.get(chat);
                                }
                                chatNode.putObject("contact").setAll(cache.get(chat.getContact()));
                                chatNode.put("picture", chat.getContact().getThumb());
                                chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
                                return chatNode;
                            }
                        });
        cacheMsgs = CacheBuilder.newBuilder()
                .maximumSize(100000)
                .refreshAfterWrite(Duration.ofHours(10))
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<>() {
                            public ObjectNode load(Message message) throws ExecutionException {
                                ObjectNode msgNode;
                                try {
                                    msgNode = cache.get(message);
                                } catch (IllegalStateException e) {
                                    cache.refresh(message);
                                    msgNode = cache.get(message);
                                }
                                if (message.getSender() != null) {
                                    msgNode.putObject("sender").setAll(cache.get(message.getSender()));
                                }
                                if (message instanceof MediaMessage) {
                                    msgNode.put("filename", ((MediaMessage) message).getFileName());
                                    if (message.getJsObject().hasProperty("pageCount") && message.getJsObject().getProperty("pageCount").isNumber()) {
                                        msgNode.put("pageCount", message.getJsObject().getProperty("pageCount").asNumber().getValue());
                                    }
                                }
                                return msgNode;
                            }
                        });
    }

    @Async
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) throws ExecutionException {
        ObjectNode chatNode;
        try {
            chatNode = cacheChats.get(chat);
        } catch (IllegalStateException e) {
            cacheChats.refresh(chat);
            chatNode = cacheChats.get(chat);
        }
        ArrayNode arrayNode = objectMapper.createArrayNode();
        List<Message> allMessages = chat.getAllMessages();
        int partitionSize = allMessages.size() < 20 ? allMessages.size() : allMessages.size() / 20;
        Collection<List<Message>> partition = Util.partition(allMessages, partitionSize);
        List<CompletableFuture<ArrayNode>> futures = new ArrayList<>();
        partition.forEach(messages -> {
            futures.add(serializadorWhatsApp.serializarMsg(messages));
        });
        Util.pegarResultadosFutures(futures).forEach(arrayNode::addAll);
        chatNode.set("msgs", arrayNode);
        chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarChat(List<Chat> chats) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        chats.forEach(chat -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarChat(chat)));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }

    @Async
    public CompletableFuture<ObjectNode> serializarMsg(Message message) throws ExecutionException {
        ObjectNode objectNode;
        try {
            objectNode = cacheMsgs.get(message);
        } catch (IllegalStateException e) {
            cacheMsgs.refresh(message);
            objectNode = cacheMsgs.get(message);
        }
        return CompletableFuture.completedFuture(objectNode);
    }

    @Async
    public CompletableFuture<ArrayNode> serializarMsg(List<Message> messages) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        messages.forEach(message -> {
            try {
                arrayNode.add(Util.pegarResultadoFuture(serializarMsg(message)));
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        return CompletableFuture.completedFuture(arrayNode);
    }
}
