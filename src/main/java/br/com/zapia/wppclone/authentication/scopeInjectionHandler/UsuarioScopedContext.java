package br.com.zapia.wppclone.authentication.scopeInjectionHandler;

import br.com.zapia.wppclone.modelo.Usuario;

public class UsuarioScopedContext {

    private final static ThreadLocal<Usuario> usuarioThreadLocal = new ThreadLocal<>();

    public static Usuario getUsuario() {
        return usuarioThreadLocal.get();
    }

    public static void setUsuario(Usuario usuario) {
        usuarioThreadLocal.set(usuario);
    }

    public static void reset() {
        usuarioThreadLocal.remove();
    }
}
