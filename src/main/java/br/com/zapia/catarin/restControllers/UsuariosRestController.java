package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.modelo.Usuario;
import br.com.zapia.catarin.servicos.CRUDService;
import br.com.zapia.catarin.servicos.UsuariosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsuariosRestController extends CRUDRest<Usuario> {

    @Autowired
    private UsuariosService usuariosService;

    public UsuariosRestController() {
        super(Usuario.class);
    }

    @PostMapping("/mudarSenha")
    public ResponseEntity<?> mudarSenha(@RequestParam String senha, @RequestParam String senhaAtual) {
        Usuario currentUser = usuariosService.buscar(getCurrentUser().getUuid());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(senhaAtual, currentUser.getSenha())) {
            currentUser.setSenha(senha);
            if (getService().salvar(currentUser)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Override
    CRUDService<Usuario> getService() {
        return usuariosService;
    }
}
