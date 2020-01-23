package br.com.zapia.wppclone.authentication.scopeInjectionHandler;

import br.com.zapia.wppclone.modelo.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsuarioContextRunnable implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(UsuarioContextRunnable.class);
    private Runnable task;
    private Usuario usuario;

    public UsuarioContextRunnable(Runnable task, Usuario usuario) {
        this.task = task;
        this.usuario = usuario;
    }

    @Override
    public void run() {
        if (usuario != null) {
            UsuarioScopedContext.setUsuario(usuario);
        } else {
            logger.warn("Usuario Null");
        }
        try {
            task.run();
        } catch (Exception e) {
            logger.error("call ", e);
            throw e;
        }
    }
}
