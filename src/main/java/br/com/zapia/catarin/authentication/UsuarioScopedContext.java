package br.com.zapia.catarin.authentication;

import br.com.zapia.catarin.modelo.Usuario;

public class UsuarioScopedContext {

    private final static ThreadLocal<Usuario> usuarioThreadLocal = new InheritableThreadLocal<>();

    public static Usuario getUsuario() {
        return usuarioThreadLocal.get();
    }

    public static void setUsuario(Usuario usuario) {
        usuarioThreadLocal.set(usuario);
    }
}
