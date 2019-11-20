package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import br.com.zapia.catarin.modelo.Usuario;

import java.util.concurrent.Callable;

public class UsuarioContextCallable<T> implements Callable<T> {
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
            System.out.println("Usuario Null");
        }
        try {
            return task.call();
        } finally {
            UsuarioScopedContext.reset();
        }
    }
}
