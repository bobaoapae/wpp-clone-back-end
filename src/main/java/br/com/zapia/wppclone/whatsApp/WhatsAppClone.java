package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextThreadPoolExecutor;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextThreadPoolScheduler;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.handlersWebSocket.HandlerWebSocket;
import br.com.zapia.wppclone.handlersWebSocket.HandlerWebSocketEvent;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.WebSocketRequest;
import br.com.zapia.wppclone.payloads.WebSocketResponse;
import br.com.zapia.wppclone.payloads.WebSocketResponseFrame;
import br.com.zapia.wppclone.servicos.SendEmailService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
import br.com.zapia.wppclone.whatsApp.controle.ControleChatsAsync;
import br.com.zapia.wppclone.ws.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.RateLimiter;
import driver.WebWhatsDriver;
import driver.WebWhatsDriverBuilder;
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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    private ApplicationContext ap;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;
    @Autowired
    private SendEmailService sendEmailService;
    private List<WebSocketSession> sessions;
    private Logger logger;
    private WebWhatsDriver driver;
    private Consumer<String> onNeedQrCode;
    private Consumer<Integer> onLowBaterry;
    private Consumer<DriverState> onChangeEstadoDriver;
    private ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch;
    private Runnable onConnect;
    private Runnable onDisconnect;
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
    private Usuario usuarioResponsavelInstancia;
    private Supplier<ExecutorService> executorServiceSupplier;
    private Supplier<ScheduledExecutorService> scheduledExecutorServiceSupplier;
    private RateLimiter rateLimiter;


    @PostConstruct
    public void init() throws Exception {
        try {
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
            File file = new File(pathCacheWebWhats + getUsuario().getUsuarioResponsavelPelaInstancia().getUuid());
            if (!file.exists()) {
                file.mkdirs();
            }
            file = new File(pathLogs + getUsuario().getUsuarioResponsavelPelaInstancia().getUuid());
            if (!file.exists()) {
                file.mkdirs();
            }
            file = new File(pathBinarios);
            if (!file.exists()) {
                file.mkdir();
            }
            logger = Logger.getLogger(WhatsAppClone.class.getName());
            sessions = new ConcurrentArrayList<>();
            onConnect = () -> {
                controleChatsAsync.clearAllChats();
                driver.getFunctions().subscribeToLowBattery(onLowBaterry);
                driver.getFunctions().getAllChats(true).thenAccept(chats -> chats.forEach(controleChatsAsync::addChat));
                driver.getFunctions().addChatListenner(c -> controleChatsAsync.addChat(c), EventType.ADD);
                driver.getFunctions().addChatListenner(chat -> {
                    serializadorWhatsApp.serializarChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TipoEventoWpp.NEW_CHAT, jsonNodes);
                    });
                }, EventType.ADD);
                driver.getFunctions().addChatListenner(chat -> {
                    serializadorWhatsApp.serializarChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TipoEventoWpp.UPDATE_CHAT, jsonNodes);
                    });
                }, EventType.CHANGE, "formattedTitle", "unreadCount", "pin", "presenceType", "shouldAppearInList", "lastPresenceAvailableTime", "customProperties");
                driver.getFunctions().addChatListenner(chat -> {
                    serializadorWhatsApp.serializarChat(chat).thenAccept(jsonNodes -> {
                        enviarEventoWpp(TipoEventoWpp.REMOVE_CHAT, jsonNodes);
                    });
                }, EventType.REMOVE);
                driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                    @Override
                    public void run(Message m) {
                        serializadorWhatsApp.serializarMsg(m).thenAccept(jsonNodes -> {
                            enviarEventoWpp(TipoEventoWpp.NEW_MSG, jsonNodes);
                        });
                    }
                }, EventType.ADD);
                driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                    @Override
                    public void run(Message m) {
                        serializadorWhatsApp.serializarMsg(m).thenAccept(jsonNodes -> {
                            enviarEventoWpp(TipoEventoWpp.REMOVE_MSG, jsonNodes);
                        });
                    }
                }, EventType.REMOVE);
                driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                    @Override
                    public void run(Message m) {
                        serializadorWhatsApp.serializarMsg(m).thenAccept(jsonNodes -> {
                            enviarEventoWpp(TipoEventoWpp.UPDATE_MSG, jsonNodes);
                        });
                    }
                }, EventType.CHANGE, "ack", "isRevoked", "oldId", "customProperties");
            };
            onLowBaterry = (e) -> {
                enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e);
            };
            onNeedQrCode = (e) -> {
                enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
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
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Envio de Email", e);
                }
            };
            usuarioResponsavelInstancia = getUsuario().getUsuarioResponsavelPelaInstancia();
            executorServiceSupplier = () -> {
                return new UsuarioContextThreadPoolExecutor(usuarioResponsavelInstancia, 100, 200,
                        10L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(), new ThreadFactory() {

                    private final AtomicInteger id = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("ExecutorWhatsDriver_" + id.getAndIncrement());
                        return thread;
                    }
                });
            };
            scheduledExecutorServiceSupplier = () -> {
                return new UsuarioContextThreadPoolScheduler(usuarioResponsavelInstancia, 50);
            };
            rateLimiter = RateLimiter.create(20);
            WebWhatsDriverBuilder builder = new WebWhatsDriverBuilder(pathCacheWebWhats + usuarioPrincipalAutoWired.getUsuario().getUsuarioResponsavelPelaInstancia().getUuid(), pathBinarios);
            builder.onConnect(onConnect);
            builder.onChangeDriverState(onChangeEstadoDriver);
            builder.customExecutorService(executorServiceSupplier);
            builder.customScheduledExecutorService(scheduledExecutorServiceSupplier);
            builder.onNeedQrCode(onNeedQrCode);
            builder.headLess(headLess);
            builder.onWhatsAppVersionMismatch(onWhatsAppVersionMismatch);
            builder.addErrorHandler(throwable -> {
                logger.log(Level.SEVERE, "Error Driver WhatsApp " + usuarioResponsavelInstancia.getLogin(), throwable);
            });
            driver = builder.build();
            whatsAppCloneService.adicionarInstancia(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Init", e);
            if (driver != null) {
                driver.finalizar();
            }
            throw e;
        }
    }

    @Async
    public void enviarParaWs(WebSocketSession ws, WsMessage message) {
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
        WebSocketSession finalSession = buscarWsDecorator(session);
        if (driver.getDriverState() != DriverState.LOGGED) {
            enviarParaWs(finalSession, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.FAILED_DEPENDENCY, "WhatsApp Not Logged")));
        } else {
            try {
                processWebSocketResponse(webSocketRequest).thenAccept(webSocketResponse -> {
                    try {
                        String dado;
                        if (webSocketResponse.getResponse() instanceof String) {
                            dado = (String) webSocketResponse.getResponse();
                        } else {
                            dado = objectMapper.writeValueAsString(webSocketResponse.getResponse());
                        }
                        int maxKb = 1024 * 1024;
                        if (dado.getBytes().length >= maxKb) {
                            List<String> partials = Util.splitStringByByteLength(dado, maxKb);
                            for (int x = 0; x < partials.size(); x++) {
                                WebSocketResponseFrame frame = new WebSocketResponseFrame(webSocketResponse.getStatus(), partials.get(x));
                                frame.setFrameId(x + 1);
                                frame.setQtdFrames(partials.size());
                                whatsAppClone.enviarParaWs(finalSession, new WsMessage(webSocketRequest, frame));
                            }
                        } else {
                            enviarParaWs(finalSession, new WsMessage(webSocketRequest, webSocketResponse));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Process WebSocket Msg", throwable);
                    enviarParaWs(finalSession, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(throwable))));
                    return null;
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Process WebSocket Msg", e);
                enviarParaWs(finalSession, new WsMessage(webSocketRequest, new WebSocketResponse(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getMessage(e))));
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

    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        getSessions().forEach(webSocketSession -> whatsAppClone.enviarEventoWpp(tipoEventoWpp, dado, webSocketSession));
    }

    @Async
    public void enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado, WebSocketSession ws) {
        enviarParaWs(ws, new WsMessage(tipoEventoWpp.name().replace("_", "-"), dado));
        if (tipoEventoWpp == TipoEventoWpp.UPDATE_ESTADO && driver.getDriverState() == DriverState.LOGGED) {
            sendInit(ws);
        }
    }

    private void sendInit(WebSocketSession ws) {
        try {
            Chat myChat = driver.getFunctions().getMyChat().get(10, TimeUnit.SECONDS);
            if (myChat != null) {
                boolean isBussiness = driver.getFunctions().isBusiness().join();
                ObjectNode dados = objectMapper.createObjectNode();
                dados.putObject("self").setAll(serializadorWhatsApp.serializarChat(myChat, true).join());
                dados.put("isBussiness", isBussiness);
                whatsAppClone.enviarEventoWpp(TipoEventoWpp.INIT, objectMapper.writeValueAsString(dados), ws);
            } else {
                driver.reiniciar();
                whatsAppClone.enviarEventoWpp(TipoEventoWpp.ERROR, "My Chat Null", ws);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SendInit", e);
            whatsAppClone.enviarEventoWpp(TipoEventoWpp.ERROR, ExceptionUtils.getStackTrace(e), ws);
        }
    }

    @Async
    public CompletableFuture<Void> logout() {
        return driver.getFunctions().logout();
    }

    public void adicionarSession(WebSocketSession ws) {
        ws.setTextMessageSizeLimit(5 * 1024 * 1024);
        ws = new ConcurrentWebSocketSessionDecorator(ws, 60000, 10 * 1024 * 1024);
        sessions.add(ws);
        whatsAppClone.enviarEventoWpp(WhatsAppClone.TipoEventoWpp.UPDATE_ESTADO, whatsAppClone.getDriver().getDriverState().name(), ws);
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

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void finalizarQuandoInativo() {
        if (!instanciaGeral && (getSessions().isEmpty() && (lastTimeWithSessions == null || lastTimeWithSessions.plusMinutes(5).isBefore(LocalDateTime.now())) || driver.getDriverState() == DriverState.WAITING_QR_CODE_SCAN)) {
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
            if (driver.getDriverState() == DriverState.LOGGED) {
                driver.getFunctions().sendPresenceUnavailable();
            }
        }
    }

    private void shutdown() {
        new Thread(() -> {
            UsuarioScopedContext.setUsuario(usuarioResponsavelInstancia);
            ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("whatsAppClone");
        }).start();
    }

    public void setForceShutdown(boolean forceShutdown) {
        this.forceShutdown = forceShutdown;
    }

    @PreDestroy
    public void preDestroy() {
        logger.info("Destroy WhatsAppClone");
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("controleChatsAsync");
        ((AbstractBeanFactory) ap.getAutowireCapableBeanFactory()).destroyScopedBean("serializadorWhatsApp");
        for (WebSocketSession webSocketSession : getSessions()) {
            if (webSocketSession.isOpen()) {
                try {
                    webSocketSession.close(CloseStatus.GOING_AWAY);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Close Ws Session", e);
                }
            }
        }
        driver.finalizar();
        whatsAppCloneService.removerInstancia(this);
    }

    public List<WebSocketSession> getSessions() {
        return sessions.stream().filter(WebSocketSession::isOpen).collect(Collectors.toUnmodifiableList());
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
}
