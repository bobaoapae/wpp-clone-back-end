package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.modelo.Usuario;
import br.com.zapia.catarin.servicos.CRUDService;
import br.com.zapia.catarin.servicos.UsuariosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsuariosRestController extends CRUDRest<Usuario> {

    @Autowired
    private UsuariosService usuariosService;

    @PostMapping(value = "/salvar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ResponseEntity<?> salvar(@RequestBody Usuario obj) {
        return super.salvar(obj);
    }

    @Override
    CRUDService<Usuario> getService() {
        return usuariosService;
    }
}
