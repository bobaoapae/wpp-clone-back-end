package br.com.zapia.wppclone.authentication;

import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;

public class UsuarioAuthentication implements UserDetails {

    private Usuario usuario;

    public UsuarioAuthentication(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public String getUsername() {
        return usuario.getLogin();
    }

    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(usuario.getPermissao());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.isAtivo() && usuario.getUsuarioResponsavelPelaInstancia().isAtivo();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo() && usuario.getUsuarioResponsavelPelaInstancia().isAtivo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioAuthentication)) return false;
        UsuarioAuthentication that = (UsuarioAuthentication) o;
        return usuario.equals(that.getUsuario());
    }

    @Override
    public int hashCode() {
        return usuario.hashCode();
    }
}
