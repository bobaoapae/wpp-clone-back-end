package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.handlersWebSocket.HandlerWebSocket;
import br.com.zapia.wppclone.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.servicos.SendEmailService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
import br.com.zapia.wppclone.whatsApp.controle.ControleChatsAsync;
import br.com.zapia.wppclone.ws.WsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import driver.WebWhatsDriver;
import modelo.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.threadly.concurrent.collections.ConcurrentArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.MessagingException;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    @Autowired
    private ApplicationContext ap;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;
    @Autowired
    private SendEmailService sendEmailService;
    private List<WebSocketSession> sessions;
    private Logger logger;
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private ActionOnChangeEstadoDriver onChangeEstadoDriver;
    private ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private TelaWhatsApp telaWhatsApp;
    @Value("${pathCacheWebWhats}")
    private String pathCacheWebWhats;
    @Value("${pathLogs}")
    private String pathLogs;
    @Value("${pathBinarios}")
    private String pathBinarios;
    @Value("${loginWhatsAppGeral}")
    private String loginWhatsAppGeral;
    @Value("${headLess}")
    private boolean headLess;
    @Value("${forceBeta}")
    private boolean forceBeta;
    private boolean forceShutdown;
    private boolean instanciaGeral;
    private ObjectMapper objectMapper;
    private Map<String, HandlerWebSocket> handlers;
    private LocalDateTime lastTimeWithSessions;


    @PostConstruct
    public void init() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!getUsuario().getUsuarioResponsavelPelaInstancia().isAtivo()) {
            throw new InstantiationException("Usuário Responsável Inativo");
        }
        instanciaGeral = getUsuario().getLogin().equals(loginWhatsAppGeral);
        objectMapper = new ObjectMapper();
        handlers = new ConcurrentHashMap<>();
        Constructor<Reflections> declaredConstructor = Reflections.class.getDeclaredConstructor();
        declaredConstructor.setAccessible(true);
        declaredConstructor.newInstance().collect(getClass().getResourceAsStream("/META-INF/reflections/reflections.xml")).getTypesAnnotatedWith(HandlerWebSocketEvent.class).forEach(aClass -> {
            try {
                handlers.put(aClass.getAnnotation(HandlerWebSocketEvent.class).event(), ap.getBean((Class<HandlerWebSocket>) aClass));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Init Handlers", e);
            }
        });
        File file = new File(pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid());
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathLogs + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid());
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
            driver.getFunctions().getAllChats(true).thenAccept(chats -> chats.forEach(controleChatsAsync::addChat));
            driver.getFunctions().addChatListenner(c -> controleChatsAsync.addChat(c), EventType.ADD, false);
            driver.getFunctions().addChatListenner(chat -> {
                enviarEventoWpp(TipoEventoWpp.NEW_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, EventType.ADD);
            driver.getFunctions().addChatListenner(chat -> {
                enviarEventoWpp(TipoEventoWpp.UPDATE_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, EventType.CHANGE, "unreadCount", "pin", "presenceType", "shouldAppearInList", "lastPresenceAvailableTime");
            driver.getFunctions().addChatListenner(chat -> {
                enviarEventoWpp(TipoEventoWpp.REMOVE_CHAT, Util.pegarResultadoFuture(serializadorWhatsApp.serializarChat(chat)));
            }, EventType.REMOVE);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(m)));
                }
            }, EventType.ADD);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.REMOVE_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(m)));
                }
            }, EventType.REMOVE);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.UPDATE_MSG, Util.pegarResultadoFuture(serializadorWhatsApp.serializarMsg(m)));
                }
            }, EventType.CHANGE, "ack", "isRevoked", "oldId");
        };
        onLowBaterry = (e) -> {
            enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e);
        };
        onNeedQrCode = (e) -> {
            enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
        };
        onErrorInDriver = (e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
            enviarEventoWpp(TipoEventoWpp.ERROR, ExceptionUtils.getStackTrace(e));
        };
        onChangeEstadoDriver = (e) -> {
            enviarEventoWpp(TipoEventoWpp.UPDATE_ESTADO, e.name());
        };
        onDisconnect = () -> {
            enviarEventoWpp(TipoEventoWpp.DISCONNECT, "Falha ao Conectar ao Telefone");
        };
        onWhatsAppVersionMismatch = (minVersion, maxVersion, actual) -> {
            try {
                sendEmailService.sendEmail("joao@zapia.com.br", "Driver API WhatsApp", "Durante a inicialização da sessão para: " +
                        "" + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getLogin() + " foi detectada uma alteração na versão do WhatsApp." +
                        "\n" +
                        "Versão Mínima da Lib: " + minVersion.toString() + "\n" +
                        "Versão Máxima da Lib: " + maxVersion.toString() + "\n" +
                        "Versão Atual do WhatsApp: " + actual.toString());
            } catch (MessagingException e) {
                logger.log(Level.SEVERE, "Envio de Email", e);
            }
        };
        if (!headLess) {
            telaWhatsApp = new TelaWhatsApp();
            telaWhatsApp.setVisible(true);
            this.driver = webWhatsDriverSpring.initialize(telaWhatsApp.getPanel(), pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid(), forceBeta, false, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        } else {
            this.driver = webWhatsDriverSpring.initialize(pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid(), forceBeta, false, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        }
        whatsAppCloneService.adicionarInstancia(this);
    }

    private void enviarParaWs(WebSocketSession ws, WsMessage message) {
        if (!(ws instanceof ConcurrentWebSocketSessionDecorator)) {
            ws = buscarWsDecorator(ws);
        }
        if (ws.isOpen()) {
            try {
                ws.sendMessage(new TextMessage(message.toString()));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "EnviarParaWs", e);
                try {
                    ws.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "CloseWs", ex);
                }
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
    public void processWebSocketMsg(WebSocketSession session, WebSocketRequest webSocketRequest) {
        lastTimeWithSessions = LocalDateTime.now();
        if (driver.getEstadoDriver() != EstadoDriver.LOGGED) {
            enviarParaWs(session, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.FAILED_DEPENDENCY, "WhatsApp Not Logged")));
        } else {
            try {
                processWebSocketResponse(webSocketRequest).thenAccept(webSocketResponse -> {
                    enviarParaWs(session, new WsMessage(webSocketRequest, webSocketResponse));
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Process WebSocket Msg", e);
                enviarParaWs(session, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(e))));
            }
        }
    }

    private CompletableFuture<WebSocketResponse> processWebSocketResponse(WebSocketRequest webSocketRequest) {
        try {
            HandlerWebSocket handlerWebSocket = handlers.get(webSocketRequest.getWebSocketRequestPayLoad().getEvent());
            if (handlerWebSocket != null) {
                return handlerWebSocket.handle((Usuario) webSocketRequest.getWebSocketSession().getAttributes().get("usuario"), webSocketRequest.getWebSocketRequestPayLoad().getPayload());
            } else {
                return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.NOT_IMPLEMENTED));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Handle WebSocketRequest", e);
            return CompletableFuture.completedFuture(new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(e)));
        }
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        getSessions().forEach(webSocketSession -> whatsAppClone.enviarEventoWpp(tipoEventoWpp, dado, webSocketSession));
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado, WebSocketSession ws) {
        enviarParaWs(ws, new WsMessage(tipoEventoWpp.name().replace("_", "-"), dado));
        if (tipoEventoWpp == TipoEventoWpp.UPDATE_ESTADO && driver.getEstadoDriver() == EstadoDriver.LOGGED) {
            sendInit(ws);
        }
    }

    public void sendInit(WebSocketSession ws) {
        driver.getFunctions().getMyChat().thenCompose(chat -> {
            if (chat != null) {
                ObjectNode dados = objectMapper.createObjectNode();
                return serializadorWhatsApp.serializarChat(chat, true).thenAccept(jsonNodes -> {
                    dados.putObject("self").setAll(jsonNodes);
                }).thenCompose(aVoid -> driver.getFunctions().isBusiness()).thenCompose(value -> {
                    if (value) {
                        return driver.getFunctions().getStoreObjectByName("QuickReply").thenAccept(jsObject -> {
                            try {
                                dados.putArray("quickReplys").addAll((ArrayNode) objectMapper.readTree(jsObject.toJSONString()));
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Serialize QuickReplys", e);
                            }
                        });
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                }).thenCompose(aVoid -> {
                    return serializadorWhatsApp.serializarAllContacts().thenAccept(jsonNodes -> {
                        dados.putArray("contacts").addAll(jsonNodes);
                    }).thenCompose(aVoid1 -> serializadorWhatsApp.serializarAllChats()).thenAccept(jsonNodes -> {
                        dados.putArray("chats").addAll(jsonNodes);
                    });
                }).thenApply(aVoid -> {
                    try {
                        return objectMapper.writeValueAsString(dados);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                driver.reiniciar();
                return CompletableFuture.completedFuture("My Chat null");
            }
        }).thenAccept(s -> {
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.INIT, s, ws);
        });
    }

    @Async
    public CompletableFuture<Void> logout() {
        return driver.getFunctions().logout();
    }

    public void adicionarSession(WebSocketSession ws) {
        ws.setTextMessageSizeLimit(100 * 1024 * 1024);
        ws = new ConcurrentWebSocketSessionDecorator(ws, 60000, 80 * 1024 * 1024);
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

    @Scheduled(fixedDelay = 120000, initialDelay = 240000)
    public void finalizarQuandoInativo() {
        if (!instanciaGeral && (getSessions().isEmpty() && (lastTimeWithSessions == null || lastTimeWithSessions.plusMinutes(10).isBefore(LocalDateTime.now())) || driver.getEstadoDriver() == EstadoDriver.WAITING_QR_CODE_SCAN)) {
            logger.info("Finalizar Instancia Inativa: " + getUsuario().getUsuarioResponsavelPelaInstancia().getLogin());
            shutdown();
        }
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 0)
    public void finalizarSeForcado() {
        if (!getUsuario().getUsuarioResponsavelPelaInstancia().isAtivo() || forceShutdown) {
            logger.info("Finalizar Instancia Forçada: " + getUsuario().getUsuarioResponsavelPelaInstancia().getLogin());
            shutdown();
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void checkPresence() {
        if (getSessions().isEmpty()) {
            if (driver.getEstadoDriver() == EstadoDriver.LOGGED) {
                driver.getFunctions().sendPresenceUnavailable();
            }
        }
    }

    private void shutdown() {
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("whatsAppClone");
    }

    public void setForceShutdown(boolean forceShutdown) {
        this.forceShutdown = forceShutdown;
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("Destroy WhatsAppClone");
        driver.finalizar();
        if (telaWhatsApp != null) {
            telaWhatsApp.dispose();
        }
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("controleChatsAsync");
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("serializadorWhatsApp");
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("webWhatsDriverSpring");
        for (WebSocketSession webSocketSession : getSessions()) {
            if (webSocketSession.isOpen()) {
                try {
                    webSocketSession.close(CloseStatus.GOING_AWAY);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Close Ws Session", e);
                }
            }
        }
        whatsAppCloneService.removerInstancia(this);
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

    public SerializadorWhatsApp getSerializadorWhatsApp() {
        return serializadorWhatsApp;
    }

    public Usuario getUsuario() {
        return usuarioPrincipalAutoWired.getUsuario();
    }

    public enum TipoEventoWpp {
        UPDATE_CHAT,
        REMOVE_CHAT,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        UPDATE_MSG,
        REMOVE_MSG,
        REMOVE_MSG_V3,
        UPDATE_MSG_V3,
        LOW_BATTERY,
        NEED_QRCODE,
        UPDATE_ESTADO,
        DISCONNECT,
        ERROR,
        CHAT_PICTURE,
        INIT
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp() {
            this.setTitle(usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getNome() + " - " + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getLogin());
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
