package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;
import br.com.zapia.wppclone.servicos.UsuariosService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/config")
public class ConfiguracaoUsuarioRestController {

    @Autowired
    private UsuariosService usuariosService;
    @Autowired
    private UsuarioPrincipalAutoWired usuario;
    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/toggleEnvioNomeOperador")
    public ResponseEntity<?> toggleEnvioNomeOperador() {
        Usuario usuario = usuariosService.buscar(this.usuario.getUsuario().getUuid());
        usuario.getConfiguracao().setEnviarNomeOperadores(!usuario.getConfiguracao().getEnviarNomeOperadores());
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
