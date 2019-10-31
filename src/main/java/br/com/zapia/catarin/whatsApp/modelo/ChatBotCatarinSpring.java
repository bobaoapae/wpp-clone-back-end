package br.com.zapia.catarin.whatsApp.modelo;

import modelo.Chat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
public class ChatBotCatarinSpring {

    @Autowired
    private ApplicationContext applicationContext;
    private ChatBotCatarin chatBotCatarin;

    public ChatBotCatarin inicializarChatBotCatarin(Chat chat, boolean autoPause) {
        if (chatBotCatarin == null) {
            chatBotCatarin = new ChatBotCatarin(this, chat, autoPause);
        }
        return chatBotCatarin;
    }

    public ChatBotCatarin getChatBotCatarin() {
        return chatBotCatarin;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
