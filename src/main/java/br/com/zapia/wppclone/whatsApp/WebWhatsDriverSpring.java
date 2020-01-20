package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextThreadPoolExecutor;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextThreadPoolScheduler;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.modelo.Usuario;
import driver.WebWhatsDriver;
import modelo.ActionOnChangeEstadoDriver;
import modelo.ActionOnErrorInDriver;
import modelo.ActionOnLowBattery;
import modelo.ActionOnNeedQrCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Scope("usuario")
@Service
public class WebWhatsDriverSpring {

    private WebWhatsDriver driver;
    private Usuario usuario;

    @PostConstruct
    public void init() {
        usuario = UsuarioScopedContext.getUsuario();
    }

    public WebWhatsDriver initialize(JPanel panel, String profilePath, boolean forceBeta, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver) throws IOException {
        this.driver = new WebWhatsDriverSpringIntern(panel, profilePath, forceBeta, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver);
        return this.driver;
    }

    public WebWhatsDriver initialize(String profilePath, boolean forceBeta, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver) throws IOException {
        this.driver = new WebWhatsDriverSpringIntern(profilePath, forceBeta, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver);
        return this.driver;
    }

    private class WebWhatsDriverSpringIntern extends WebWhatsDriver {

        public WebWhatsDriverSpringIntern(JPanel panel, String profilePath, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver) throws IOException {
            super(panel, profilePath, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver);
        }

        public WebWhatsDriverSpringIntern(JPanel panel, String profilePath, boolean forceBeta, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver) throws IOException {
            super(panel, profilePath, forceBeta, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver);
        }

        public WebWhatsDriverSpringIntern(String profilePath, boolean forceBeta, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver) throws IOException {
            super(profilePath, forceBeta, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver);
        }

        public WebWhatsDriverSpringIntern(String profilePath, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver) throws IOException {
            super(profilePath, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver);
        }

        @Override
        public ScheduledThreadPoolExecutor getScheduler() {
            return new UsuarioContextThreadPoolScheduler(usuario, 10);
        }

        @Override
        public ExecutorService getExecutorServiceInterno() {
            return new UsuarioContextThreadPoolExecutor(usuario, 10, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());
        }
    }
}
