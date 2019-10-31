/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package br.com.zapia.catarin.whatsApp.modelo;

import handlersBot.HandlerBot;
import modelo.Chat;
import modelo.ChatBot;
import modelo.Message;

public class ChatBotCatarin extends ChatBot {

    private ChatBotCatarinSpring chatBotCatarinSpring;

    public ChatBotCatarin(ChatBotCatarinSpring chatBotCatarinSpring, Chat chat, boolean autoPause) {
        super(chat, autoPause);
        this.chatBotCatarinSpring = chatBotCatarinSpring;
    }

    public ChatBotCatarinSpring getChatBotCatarinSpring() {
        return chatBotCatarinSpring;
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
        if (getChat().getDriver().getFunctions().isBusiness()) {
            getChat().removeLabel("Precisa de Ajuda");
        }
    }

    @Override
    public void processNewMsg(Message m) {

    }

    @Override
    public void processNewStatusV3Msg(Message message) {

    }

    public void finalizar() {
        if (this.checkMsgs != null && !this.checkMsgs.isCancelled() && !this.checkMsgs.isDone()) {
            this.checkMsgs.cancel(true);
        }
        if (this.checkMsgsStatusV3 != null && !this.checkMsgsStatusV3.isCancelled() && !this.checkMsgsStatusV3.isDone()) {
            this.checkMsgsStatusV3.cancel(true);
        }
        this.executor.shutdown();
    }
}
