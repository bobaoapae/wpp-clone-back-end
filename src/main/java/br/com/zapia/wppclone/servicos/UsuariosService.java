package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.authentication.UsuarioAuthentication;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.repositorios.CRUDRepository;
import br.com.zapia.wppclone.repositorios.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;

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

    public List<Usuario> listarUsuariosFilhos(Usuario usuarioPai) {
        return usuariosRepository.listarUsuariosFilhos(usuarioPai);
    }
}
