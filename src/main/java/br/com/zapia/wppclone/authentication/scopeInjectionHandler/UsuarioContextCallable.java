package br.com.zapia.wppclone.authentication.scopeInjectionHandler;

import br.com.zapia.wppclone.modelo.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class UsuarioContextCallable<T> implements Callable<T> {

    private static Logger logger = LoggerFactory.getLogger(UsuarioContextCallable.class);
    private Callable<T> task;
    private Usuario usuario;

    public UsuarioContextCallable(Callable<T> task, Usuario usuario) {
        this.task = task;
        this.usuario = usuario;
    }

    @Override
    public T call() throws Exception {
        if (usuario != null) {
            UsuarioScopedContext.setUsuario(usuario);
        } else {
            logger.warn("Usuario Null");
        }
        try {
            return task.call();
        } catch (Exception e) {
            logger.error("call ", e);
            throw e;
        } finally {
            UsuarioScopedContext.reset();
        }
    }
}
