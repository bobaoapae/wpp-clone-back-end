package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.dto.DTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioBasicResponseDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioCreateDTO;
import br.com.zapia.wppclone.servicos.OperadoresService;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.utils.Util;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operators")
@Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_USER"})
public class OperadorRestController {

    @Autowired
    private UsuariosService usuariosService;
    @Autowired
    private PermissoesService permissoesService;
    @Autowired
    private UsuarioPrincipalAutoWired usuario;
    @Autowired
    private OperadoresService operadoresService;
    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<?> criarNovoOperador(@DTO(UsuarioCreateDTO.class) Usuario usuario) {
        usuario.setUsuarioPai(this.usuario.getUsuario());
        usuario.setPermissao(permissoesService.buscarPermissaoPorNome("ROLE_OPERATOR"));
        usuario.setLogin(usuario.getLogin().replace(usuario.getUsuarioPai().getLogin().concat("/"), ""));
        usuario.setLogin(usuario.getUsuarioPai().getLogin().concat("/").concat(usuario.getLogin()));
        usuario.setTelefone("000000000"); //TODO remover
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioBasicResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/resetPassword/{uuid}")
    public ResponseEntity<?> resetarSenha(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            String newPassword = Util.generateRandomString(10, false);
            usuario.setSenha(newPassword);
            if (usuariosService.salvar(usuario)) {
                return ResponseEntity.ok().body(newPassword);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<?> deletarOperador(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            if (usuario.getUsuarioPai().equals(this.usuario.getUsuario())) {
                if (usuariosService.remover(usuario)) {
                    operadoresService.finalizarSessoesParaOperadorSeEstiveremAtivas(usuario);
                    return ResponseEntity.ok().build();
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTodos() {
        List<Usuario> listar = usuariosService.listarOperadores(usuario.getUsuario());
        return ResponseEntity.ok(modelMapper.map(listar, new TypeToken<List<UsuarioBasicResponseDTO>>() {
        }.getType()));
    }
}
