package br.com.zapia.catarin.authentication;

import br.com.zapia.catarin.modelo.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPrincipalAutoWired {

    public Usuario getUsuario() {
        return (Usuario) ((UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}
