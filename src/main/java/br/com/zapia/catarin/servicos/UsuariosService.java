package br.com.zapia.catarin.servicos;

import br.com.zapia.catarin.authentication.UsuarioAuthentication;
import br.com.zapia.catarin.modelo.Usuario;
import br.com.zapia.catarin.repositorios.CRUDRepository;
import br.com.zapia.catarin.repositorios.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;

@Service
@Transactional
public class UsuariosService extends CRUDService<Usuario> implements UserDetailsService {

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Override
    public CRUDRepository<Usuario> getRepository() {
        return usuariosRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuariosRepository.buscarUsuarioPorLogin(login);
        UsuarioAuthentication usuarioAuthentication = new UsuarioAuthentication();
        usuarioAuthentication.setSenha(usuario.getSenha());
        usuarioAuthentication.setLogin(usuario.getLogin());
        usuarioAuthentication.setUuid(usuario.getUuid());
        usuarioAuthentication.setNome(usuario.getNome());
        usuarioAuthentication.setAuthorities(Arrays.asList(usuario.getPermissao()));
        return usuarioAuthentication;
    }
}
