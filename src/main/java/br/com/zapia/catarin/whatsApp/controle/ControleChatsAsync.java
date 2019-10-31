package br.com.zapia.catarin.whatsApp.controle;

import br.com.zapia.catarin.whatsApp.modelo.ChatBotCatarinSpring;
import modelo.Chat;
import modelo.UserChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@Scope("singleton")
public class ControleChatsAsync {


    private final List<ChatBotCatarinSpring> chats;
    @Autowired
    private ApplicationContext applicationContext;

    private ControleChatsAsync() {
        this.chats = Collections.synchronizedList(new ArrayList<>());
    }

    public ChatBotCatarinSpring addChat(Chat chat) {
        synchronized (chats) {
            try {
                if (chat instanceof UserChat) {
                    ChatBotCatarinSpring bean = applicationContext.getBean(ChatBotCatarinSpring.class);
                    bean.inicializarChatBotCatarin(chat, true);
                    chats.add(bean);
                    return bean;
                }
            } catch (Exception ex) {
                chat.getDriver().onError(ex);
            }
            return null;
        }
    }

    public ChatBotCatarinSpring getChatAsyncByChat(Chat chat) {
        synchronized (chats) {
            for (ChatBotCatarinSpring chatt : chats) {
                if (chatt.getChatBotCatarin().getChat().equals(chat)) {
                    return chatt;
                }
            }
        }
        return addChat(chat);
    }

    public ChatBotCatarinSpring getChatAsyncByChat(String chatid) {
        synchronized (chats) {
            for (ChatBotCatarinSpring chatt : chats) {
                if (chatt.getChatBotCatarin().getChat().getId().equals(chatid)) {
                    return chatt;
                }
            }
            return null;
        }
    }

    public List<ChatBotCatarinSpring> getChats() {
        return chats;
    }

    public void finalizar() {
        for (ChatBotCatarinSpring chatt : chats) {
            chatt.getChatBotCatarin().finalizar();
        }
    }
}
