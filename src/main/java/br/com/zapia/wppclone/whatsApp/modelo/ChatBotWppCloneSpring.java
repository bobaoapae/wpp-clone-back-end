package br.com.zapia.wppclone.whatsApp.modelo;

import modelo.Chat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype")
public class ChatBotWppCloneSpring {

    @Autowired
    private ApplicationContext applicationContext;
    private ChatBotWppClone chatBotWppClone;

    public ChatBotWppClone inicializarChatBotWppClone(Chat chat) {
        if (chatBotWppClone == null) {
            chatBotWppClone = new ChatBotWppClone(this, chat);
        }
        return chatBotWppClone;
    }

    public ChatBotWppClone getChatBotWppClone() {
        return chatBotWppClone;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
