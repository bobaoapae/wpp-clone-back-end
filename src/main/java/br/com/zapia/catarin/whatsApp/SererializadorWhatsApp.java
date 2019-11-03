package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import modelo.Chat;
import modelo.Message;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SererializadorWhatsApp implements ApplicationContextAware {

    @Lazy
    @Autowired
    private CatarinWhatsApp catarinWhatsApp;
    private ObjectMapper objectMapper;
    private ApplicationContext applicationContext;
    private SererializadorWhatsApp sererializadorWhatsApp;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ObjectNode> serializarChat(Chat chat) throws IOException {
        ObjectNode chatNode = (ObjectNode) objectMapper.readTree(chat.toJson());
        ArrayNode msgsNode = objectMapper.createArrayNode();
        List<CompletableFuture<ObjectNode>> futures = new ArrayList<>();
        Collection<List<Message>> partition = Util.partition(chat.getAllMessages(), 5);
        partition.forEach(messages -> {
            messages.forEach(message -> {
                try {
                    futures.add(getSererializadorWhatsApp().serializarMsg(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
        Util.pegarResultadosFutures(futures).forEach(msgsNode::add);
        chatNode.set("msgs", msgsNode);
        chatNode.putObject("contact").setAll((ObjectNode) objectMapper.readTree(chat.getContact().toJson()));
        chatNode.put("picture", chat.getContact().getThumb());
        chatNode.put("type", chat.getJsObject().getProperty("kind").asString().getValue());
        chatNode.put("noEarlierMsgs", chat.noEarlierMsgs());
        if (chat.getAllMessages().size() < 5 && !chat.noEarlierMsgs()) {
            chat.loadEarlierMsgs(() -> {
                catarinWhatsApp.runAfterInit(() -> {
                    try {
                        catarinWhatsApp.enviarEventoWpp(CatarinWhatsApp.TipoEventoWpp.CHAT_UPDATE, Util.pegarResultadoFuture(getSererializadorWhatsApp().serializarChat(chat)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        }
        return CompletableFuture.completedFuture(chatNode);
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<ObjectNode> serializarMsg(Message message) throws IOException {
        ObjectNode msgNode = (ObjectNode) objectMapper.readTree(message.toJson());
        if (message.getSender() != null) {
            msgNode.putObject("sender").setAll((ObjectNode) objectMapper.readTree(message.getSender().toJson()));
        }
        if (message.getChat() != null) {
            msgNode.put("unreadCount", message.getChat().getJsObject().getProperty("unreadCount").asNumber().getValue());
        }
        return CompletableFuture.completedFuture(msgNode);
    }

    public SererializadorWhatsApp getSererializadorWhatsApp() {
        if (this.sererializadorWhatsApp == null) {
            this.sererializadorWhatsApp = this.applicationContext.getBean(SererializadorWhatsApp.class);
        }
        return this.sererializadorWhatsApp;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
