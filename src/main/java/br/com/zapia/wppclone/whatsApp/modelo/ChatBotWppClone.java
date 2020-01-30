/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package br.com.zapia.wppclone.whatsApp.modelo;

import ezvcard.parameter.TelephoneType;
import handlersBot.HandlerBot;
import modelo.Chat;
import modelo.ChatBot;
import modelo.Message;

public class ChatBotWppClone extends ChatBot {

    private ChatBotWppCloneSpring chatBotWppCloneSpring;

    public ChatBotWppClone(ChatBotWppCloneSpring chatBotWppCloneSpring, Chat chat) {
        super(chat);
        this.chatBotWppCloneSpring = chatBotWppCloneSpring;
    }

    public ChatBotWppCloneSpring getChatBotWppCloneSpring() {
        return chatBotWppCloneSpring;
    }

    @Override
    public HandlerBot getHandler() {
        return super.getHandler();
    }

    @Override
    public boolean sendRequestAjuda() {
        return false;
    }

    @Override
    public void onResume() {

    }

    @Override
    public void processNewMsg(Message m) {
        if (m.getContent().equals("/vcard")) {
            chat.sendContact("João Vitor Borges", "5544991050665", TelephoneType.CELL);
        }
    }


    public void finalizar() {
        if (this.checkMsgs != null && !this.checkMsgs.isCancelled() && !this.checkMsgs.isDone()) {
            this.checkMsgs.cancel(true);
        }
        this.executor.shutdown();
    }
}
