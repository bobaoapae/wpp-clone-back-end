package br.com.zapia.wppclone.authentication;

import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPrincipalAutoWired {

    public Usuario getUsuario() {
        return UsuarioScopedContext.getUsuario();
    }
}
