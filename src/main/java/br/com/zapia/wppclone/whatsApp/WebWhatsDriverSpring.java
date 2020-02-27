package br.com.zapia.wppclone.whatsApp;

import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextThreadPoolExecutor;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioContextThreadPoolScheduler;
import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.modelo.Usuario;
import driver.WebWhatsDriver;
import modelo.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@Scope("usuario")
@Service
public class WebWhatsDriverSpring {

    private WebWhatsDriver driver;
    private Usuario usuario;

    @PostConstruct
    public void init() {
        usuario = UsuarioScopedContext.getUsuario().getUsuarioResponsavelPelaInstancia();
    }

    public WebWhatsDriver initialize(JPanel panel, String profilePath, boolean forceBeta, boolean alwaysOnline, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver, ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch) throws IOException {
        this.driver = new WebWhatsDriverSpringIntern(panel, profilePath, forceBeta, alwaysOnline, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        return this.driver;
    }

    public WebWhatsDriver initialize(String profilePath, boolean forceBeta, boolean alwaysOnline, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver, ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch) throws IOException {
        this.driver = new WebWhatsDriverSpringIntern(profilePath, forceBeta, alwaysOnline, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        return this.driver;
    }

    private class WebWhatsDriverSpringIntern extends WebWhatsDriver {

        public WebWhatsDriverSpringIntern(JPanel panel, String profilePath, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver, ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch) throws IOException {
            super(panel, profilePath, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        }

        public WebWhatsDriverSpringIntern(JPanel panel, String profilePath, boolean forceBeta, boolean alwaysOnline, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver, ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch) throws IOException {
            super(panel, profilePath, forceBeta, alwaysOnline, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        }

        public WebWhatsDriverSpringIntern(String profilePath, boolean forceBeta, boolean alwaysOnline, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver, ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch) throws IOException {
            super(profilePath, forceBeta, alwaysOnline, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        }

        public WebWhatsDriverSpringIntern(String profilePath, Runnable onConnect, ActionOnNeedQrCode onNeedQrCode, ActionOnErrorInDriver onError, ActionOnLowBattery onLowBaterry, Runnable onSmarthphoneDisconnect, ActionOnChangeEstadoDriver onChangeEstadoDriver, ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch) throws IOException {
            super(profilePath, onConnect, onNeedQrCode, onError, onLowBaterry, onSmarthphoneDisconnect, onChangeEstadoDriver, onWhatsAppVersionMismatch);
        }

        @Override
        public ScheduledExecutorService getScheduler() {
            return new UsuarioContextThreadPoolScheduler(usuario, 100);
        }

        @Override
        public ExecutorService getExecutorServiceInterno() {
            return new UsuarioContextThreadPoolExecutor(usuario, 100, Integer.MAX_VALUE,
                    10L, TimeUnit.SECONDS,
                    new SynchronousQueue<>());
        }
    }
}
