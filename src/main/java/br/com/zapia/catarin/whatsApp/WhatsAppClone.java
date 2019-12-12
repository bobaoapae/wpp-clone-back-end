package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.catarin.payloads.DeleteMessageRequest;
import br.com.zapia.catarin.payloads.ForwardMessagesRequest;
import br.com.zapia.catarin.payloads.SendMessageRequest;
import br.com.zapia.catarin.utils.Util;
import br.com.zapia.catarin.whatsApp.controle.ControleChatsAsync;
import br.com.zapia.catarin.ws.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import driver.WebWhatsDriver;
import modelo.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.threadly.concurrent.collections.ConcurrentArrayList;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Component
@Scope("usuario")
public class WhatsAppClone {

    @Autowired
    private UsuarioPrincipalAutoWired usuarioPrincipalAutoWired;
    @Autowired
    private ControleChatsAsync controleChatsAsync;
    @Autowired
    private SerializadorWhatsApp serializadorWhatsApp;
    @Lazy
    @Autowired
    private WhatsAppClone whatsAppClone;
    @Autowired
    private WebWhatsDriverSpring webWhatsDriverSpring;
    private List<WebSocketSession> sessions;
    private Logger logger;
    private StdSchedulerFactory schedulerFactory;
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private ActionOnChangeEstadoDriver onChangeEstadoDriver;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private Scheduler scheduler;
    private TelaWhatsApp telaWhatsApp;
    @Value("${pathCacheWebWhats}")
    private String pathCacheWebWhats;
    @Value("${pathLogs}")
    private String pathLogs;
    @Value("${pathBinarios}")
    private String pathBinarios;
    private ObjectMapper objectMapper;


    @PostConstruct
    public void init() throws IOException {
        objectMapper = new ObjectMapper();
        File file = new File(pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUuid());
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathLogs + usuarioPrincipalAutoWired.getUsuario().getUuid());
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathBinarios);
        if (!file.exists()) {
            file.mkdir();
        }
        System.setProperty("jxbrowser.chromium.dir", pathBinarios);
        logger = Logger.getLogger(WhatsAppClone.class.getName());
        sessions = new ConcurrentArrayList<>();
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                controleChatsAsync.addChat(chat);
            }
            driver.getFunctions().addListennerToNewChat(chat -> controleChatsAsync.addChat(chat));
            driver.getFunctions().addListennerToNewChat(chat -> {
                enviarEventoWpp(TipoEventoWpp.NEW_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, true);
            driver.getFunctions().addListennerToUpdateChat(chat -> {
                enviarEventoWpp(TipoEventoWpp.CHAT_UPDATE, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            });
            driver.getFunctions().addListennerToNewMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(msg)));
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG_V3, msg.toJson());
                }
            });

            driver.getFunctions().addListennerToChangeMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.UPDATE_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(msg)));
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.UPDATE_MSG_V3, msg.toJson());
                }
            });
        };
        onLowBaterry = (e) -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e + "");
        };
        onNeedQrCode = (e) -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
        };
        onErrorInDriver = (e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.ERROR, ExceptionUtils.getStackTrace(e));
        };
        onChangeEstadoDriver = (e) -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.UPDATE_ESTADO, e.name());
        };
        telaWhatsApp = new TelaWhatsApp();
        telaWhatsApp.setVisible(true);
        this.driver = webWhatsDriverSpring.initialize(telaWhatsApp.getPanel(), pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUuid(), onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver);
        schedulerFactory = new StdSchedulerFactory();
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "WppWebClone" + usuarioPrincipalAutoWired.getUsuario().getUuid());
        properties.put("org.quartz.scheduler.instanceId", "AUTO");
        properties.put("org.quartz.threadPool.threadCount", "2");
        try {
            schedulerFactory.initialize(properties);
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 10000L)
    public void enviarNotificacaoParaTestarConexao() {
        broadcastParaWs(new WsMessage("ping", System.currentTimeMillis()));
    }

    @Async
    public void broadcastParaWs(WsMessage message) {
        getSessions().stream().filter(WebSocketSession::isOpen).forEach(webSocketSession -> {
            whatsAppClone.enviarParaWs(webSocketSession, message);
        });
    }

    @Async
    public void enviarParaWs(WebSocketSession ws, WsMessage message) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            ws = buscarWsDecorator(ws);
        }
        if (ws.isOpen()) {
            try {
                ws.sendMessage(new TextMessage(message.toString()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "EnviarParaWs", e);
            }
        } else {
            removerSession(ws);
        }
    }

    private WebSocketSession buscarWsDecorator(WebSocketSession ws) {
        return sessions.stream().filter(webSocketSession -> {
            ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator = (ConcurrentWebSocketSessionDecorator) webSocketSession;
            return concurrentWebSocketSessionDecorator.getDelegate().equals(ws);
        }).findFirst().orElse(ws);
    }

    @Async
    public CompletableFuture<Void> processWebSocketMsg(WebSocketSession session, String[] dataResponse) {
        if (driver.getEstadoDriver() != EstadoDriver.LOGGED) {
            enviarParaWs(session, new WsMessage(dataResponse[1], 401));
        } else {
            try {
                switch (dataResponse[0]) {
                    case "updatePicture": {
                        serializadorWhatsApp.updatePictureChat(this, session, dataResponse[1]);
                        break;
                    }
                    case "seeChat": {
                        Chat chatById = driver.getFunctions().getChatById(dataResponse[1]);
                        if (chatById != null) {
                            chatById.sendSeen(false);
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], 404));
                        }
                    }
                    case "loadEarly": {
                        Chat chatById = driver.getFunctions().getChatById(dataResponse[1]);
                        if (chatById != null) {
                            chatById.loadEarlierMsgs();
                            enviarEventoWpp(TipoEventoWpp.CHAT_UPDATE, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chatById)));
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], 404));
                        }
                        break;
                    }
                    case "sendMessage": {
                        SendMessageRequest sendMessageRequest = objectMapper.readValue(dataResponse[1], SendMessageRequest.class);
                        if (sendMessageRequest.getChatId() != null && !sendMessageRequest.getChatId().isEmpty()) {
                            Chat chat = driver.getFunctions().getChatById(sendMessageRequest.getChatId());
                            if (chat != null) {
                                Message message = null;
                                if (!Strings.isNullOrEmpty(sendMessageRequest.getQuotedMsg())) {
                                    message = driver.getFunctions().getMessageById(sendMessageRequest.getQuotedMsg());
                                    if (message == null) {
                                        enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.NOT_FOUND));
                                    }
                                }
                                if (sendMessageRequest.getMedia() == null || sendMessageRequest.getMedia().isEmpty()) {
                                    if (message != null) {
                                        message.replyMessage(sendMessageRequest.getMessage());
                                    } else {
                                        chat.sendMessage(sendMessageRequest.getMessage());
                                    }
                                } else if (sendMessageRequest.getFileName() != null && !sendMessageRequest.getFileName().isEmpty()) {
                                    if (message != null) {
                                        message.replyMessageWithFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), sendMessageRequest.getMessage());
                                    } else {
                                        chat.sendFile(sendMessageRequest.getMedia(), sendMessageRequest.getFileName(), sendMessageRequest.getMessage());
                                    }
                                } else {
                                    enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.BAD_REQUEST));
                                }
                            } else {
                                enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.NOT_FOUND));
                            }
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.BAD_REQUEST));
                        }
                        break;
                    }
                    case "deleteMessage": {
                        DeleteMessageRequest deleteMessageRequest = objectMapper.readValue(dataResponse[1], DeleteMessageRequest.class);
                        Message message = driver.getFunctions().getMessageById(deleteMessageRequest.getMsgId());
                        if (message != null) {
                            message.deleteMessage(deleteMessageRequest.isFromAll());
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.NOT_FOUND));
                        }
                        break;
                    }
                    case "forwardMessage": {
                        ForwardMessagesRequest forwardMessagesRequest = objectMapper.readValue(dataResponse[1], ForwardMessagesRequest.class);
                        List<Chat> chats = new ArrayList<>();
                        List<Message> msgs = new ArrayList<>();
                        for (String idMsg : forwardMessagesRequest.getIdsMsgs()) {
                            Message msg = driver.getFunctions().getMessageById(idMsg);
                            if (msg != null) {
                                msgs.add(msg);
                            }
                        }
                        for (String idChat : forwardMessagesRequest.getIdsChats()) {
                            Chat chat = driver.getFunctions().getChatById(idChat);
                            if (chat != null) {
                                chats.add(chat);
                            }
                        }
                        if (chats.size() > 0 && msgs.size() > 0) {
                            msgs.get(0).getChat().forwardMessage(msgs.toArray(Message[]::new), chats.toArray(Chat[]::new));
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.BAD_REQUEST));
                        }
                        break;
                    }
                    case "pinChat": {
                        Chat chat = driver.getFunctions().getChatById(dataResponse[1]);
                        if (chat != null) {
                            chat.setPin(true);
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.NOT_FOUND));
                        }
                        break;
                    }
                    case "unpinChat": {
                        Chat chat = driver.getFunctions().getChatById(dataResponse[1]);
                        if (chat != null) {
                            chat.setPin(false);
                        } else {
                            enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.NOT_FOUND));
                        }
                        break;
                    }
                    case "logout": {
                        logout();
                        break;
                    }
                    default: {
                        enviarParaWs(session, new WsMessage(dataResponse[1], HttpStatus.NOT_IMPLEMENTED));
                    }
                }
            } catch (Exception e) {
                enviarEventoWpp(TipoEventoWpp.ERROR, e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        getSessions().forEach(webSocketSession -> whatsAppClone.enviarEventoWpp(tipoEventoWpp, dado, webSocketSession));
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado, WebSocketSession ws) {
        whatsAppClone.enviarParaWs(ws, new WsMessage(tipoEventoWpp.name().replace("_", "-"), dado));
        if (tipoEventoWpp == TipoEventoWpp.UPDATE_ESTADO && driver.getEstadoDriver() == EstadoDriver.LOGGED) {
            whatsAppClone.sendInit(ws);
        }
    }

    @Async
    public void sendInit(WebSocketSession ws) {
        try {
            ObjectNode dados = objectMapper.createObjectNode();
            Chat myChat = driver.getFunctions().getMyChat();
            if (myChat == null) {
                driver.reiniciar();
            } else {
                dados.putObject("self").setAll(Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(myChat, true)));
                if (driver.getFunctions().isBusiness()) {
                    driver.getFunctions().getStoreObjectByName("QuickReply").ifPresent(jsObject -> {
                        try {
                            dados.putArray("quickReplys").addAll((ArrayNode) objectMapper.readTree(jsObject.toJSONString()));
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Serialize QuickReplys", e);
                        }
                    });
                }
                dados.putArray("chats").addAll(Util.pegarResultadoFuture(serializadorWhatsApp.serializarAllChats(driver)));
                whatsAppClone.enviarEventoWpp(TipoEventoWpp.INIT, objectMapper.writeValueAsString(dados), ws);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SendInit", e);
        }
    }

    public void enviarMesagemParaTecnico(String msg) {
        Chat c = driver.getFunctions().getChatByNumber("554491050665");
        String mensagem = "*Catarin:* " + msg;
        if (c != null) {
            c.sendMessage(mensagem);
            c.setArchive(true);
        }
    }

    public byte[] zip(final String str) {
        if ((str == null) || (str.length() == 0)) {
            throw new IllegalArgumentException("Cannot zip null or empty string");
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to zip content", e);
        }
    }

    @Async
    public CompletableFuture<Void> logout() {
        driver.getFunctions().logout();
        return CompletableFuture.completedFuture(null);
    }

    public void adicionarSession(WebSocketSession ws) {
        ws.setTextMessageSizeLimit(100 * 1024 * 1024);
        ws = new ConcurrentWebSocketSessionDecorator(ws, 60000, 10 * 1024 * 1024);
        sessions.add(ws);
        whatsAppClone.enviarEventoWpp(WhatsAppClone.TipoEventoWpp.UPDATE_ESTADO, whatsAppClone.getDriver().getEstadoDriver().name(), ws);
    }

    public void removerSession(WebSocketSession ws) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            sessions.removeAll(sessions.stream().filter(webSocketSession -> {
                ConcurrentWebSocketSessionDecorator concurrentWebSocketSessionDecorator = (ConcurrentWebSocketSessionDecorator) webSocketSession;
                return concurrentWebSocketSessionDecorator.getDelegate().equals(ws);
            }).collect(Collectors.toList()));
        } else {
            sessions.remove(ws);
        }
    }

    public List<WebSocketSession> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public WebWhatsDriver getDriver() {
        return driver;
    }

    public Logger getLogger() {
        return logger;
    }

    public enum TipoEventoWpp {
        CHAT_UPDATE,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        UPDATE_MSG,
        UPDATE_MSG_V3,
        LOW_BATTERY,
        NEED_QRCODE,
        UPDATE_ESTADO,
        ERROR,
        CHAT_PICTURE,
        INIT
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp() {
            this.setTitle(usuarioPrincipalAutoWired.getUsuario().getNome() + " - " + usuarioPrincipalAutoWired.getUsuario().getLogin());
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.getContentPane().setLayout(new BorderLayout());
            this.setMinimumSize(new Dimension(800, 600));
            this.setPreferredSize(new Dimension(800, 600));
            panel = new JPanel(new BorderLayout());
            this.add(panel);
            pack();
            this.setExtendedState(JFrame.ICONIFIED);
            this.setLocationRelativeTo(null);
        }

        public JPanel getPanel() {
            return panel;
        }
    }
}
