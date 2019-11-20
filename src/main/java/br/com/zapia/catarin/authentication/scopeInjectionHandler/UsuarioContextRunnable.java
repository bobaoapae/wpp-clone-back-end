package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import br.com.zapia.catarin.modelo.Usuario;

public class UsuarioContextRunnable implements Runnable {
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
            System.out.println("Usuario Null");
        }
        try {
            task.run();
        } finally {
            UsuarioScopedContext.reset();
        }
    }
}
