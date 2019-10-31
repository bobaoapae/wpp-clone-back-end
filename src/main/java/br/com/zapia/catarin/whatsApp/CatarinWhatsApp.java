package br.com.zapia.catarin.whatsApp;

import br.com.zapia.catarin.payloads.Notification;
import br.com.zapia.catarin.restControllers.NotificationRestController;
import br.com.zapia.catarin.whatsApp.controle.ControleChatsAsync;
import driver.WebWhatsDriver;
import modelo.*;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope("singleton")
public class CatarinWhatsApp {

    @Autowired
    private NotificationRestController notificationRestController;
    @Autowired
    private ControleChatsAsync controleChatsAsync;
    private Logger logger;
    private StdSchedulerFactory schedulerFactory;
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private ScheduledExecutorService executores = Executors.newScheduledThreadPool(5);
    private Scheduler scheduler;
    private TelaWhatsApp telaWhatsApp;
    @Value("${pathCacheWebWhats}")
    private String pathCacheWebWhats;
    @Value("${pathLogs}")
    private String pathLogs;
    @Value("${pathBinarios}")
    private String pathBinarios;


    @PostConstruct
    public void init() throws IOException {
        File file = new File(pathCacheWebWhats);
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathLogs);
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(pathBinarios);
        if (!file.exists()) {
            file.mkdir();
        }
        System.setProperty("jxbrowser.chromium.dir", pathBinarios);
        logger = Logger.getLogger(CatarinWhatsApp.class.getName());
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                controleChatsAsync.addChat(chat);
            }
            driver.getFunctions().addListennerToNewChat(chat -> controleChatsAsync.addChat(chat));
            driver.getFunctions().addListennerToNewChat(c -> {
                enviarEventoWpp(TipoEventoWpp.NEW_CHAT, c.toJson());
            });
            driver.getFunctions().addListennerToNewMsg(new MessageObserverIncludeMe() {

                @Override
                public void onNewMsg(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG, msg.toJson());
                }

                @Override
                public void onNewStatusV3(Message msg) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG_V3, msg.toJson());
                }
            });
        };
        onLowBaterry = (e) -> {
            enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e + "");
        };
        onNeedQrCode = (e) -> {
            enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
        };
        onErrorInDriver = (e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
        };
        telaWhatsApp = new TelaWhatsApp();
        telaWhatsApp.setVisible(true);
        this.driver = new WebWhatsDriver(telaWhatsApp.getPanel(), pathCacheWebWhats, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect);
        executores.scheduleWithFixedDelay(() -> {
            notificationRestController.sendNotification(new Notification("none", ""));
        }, 0, 20, TimeUnit.SECONDS);
        schedulerFactory = new StdSchedulerFactory();
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "Catarin");
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

    public WebWhatsDriver getDriver() {
        return driver;
    }

    public Logger getLogger() {
        return logger;
    }

    public void logout() {
        driver.getFunctions().logout();
    }

    public void enviarMesagemParaTecnico(String msg) {
        Chat c = driver.getFunctions().getChatByNumber("554491050665");
        String mensagem = "*Catarin:* " + msg;
        if (c != null) {
            c.sendMessage(mensagem);
            c.setArchive(true);
        }
    }

    public CompletionStage<?> enviarEventoWpp(TipoEventoWpp tipoEventoWpp, Object dado) {
        notificationRestController.sendNotification(new Notification(tipoEventoWpp.name().replace("_", "-"), dado));
        return null;
    }

    public boolean possuiEmitters() {
        return notificationRestController.possuiEmitters();
    }

    public enum TipoEventoWpp {
        CHAT_UPDATE,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        LOW_BATTERY,
        NEED_QRCODE
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp() {
            this.setTitle("Catarin");
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.getContentPane().setLayout(new BorderLayout());
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
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
