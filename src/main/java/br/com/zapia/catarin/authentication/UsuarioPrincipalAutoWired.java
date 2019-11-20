package br.com.zapia.catarin.authentication;

import br.com.zapia.catarin.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.catarin.modelo.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPrincipalAutoWired {

    public Usuario getUsuario() {
        return UsuarioScopedContext.getUsuario();
    }
}
