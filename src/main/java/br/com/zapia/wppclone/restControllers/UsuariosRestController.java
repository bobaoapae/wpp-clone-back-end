package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.dto.DTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioCreateDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioUpdateDTO;
import br.com.zapia.wppclone.servicos.UsuariosService;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsuariosRestController {

    @Autowired
    private UsuariosService usuariosService;
    @Autowired
    private UsuarioPrincipalAutoWired usuario;
    @Autowired
    private ModelMapper modelMapper;

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @PostMapping
    public ResponseEntity<?> criarNovoUsuario(@DTO(UsuarioCreateDTO.class) Usuario usuario) {
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @PutMapping
    public ResponseEntity<?> atualizarUsuario(@DTO(UsuarioUpdateDTO.class) Usuario usuario) {
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @DeleteMapping("/{uuid}")
    public ResponseEntity<?> deletarUsuario(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            if (usuariosService.remover(usuario)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<?> verUsuario(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        List<Usuario> listar = usuariosService.listar();
        return ResponseEntity.ok(modelMapper.map(listar, new TypeToken<List<UsuarioResponseDTO>>() {
        }.getType()));
    }

    @PostMapping("/mudarSenha")
    public ResponseEntity<?> mudarSenha(@RequestParam String senha, @RequestParam String senhaAtual) {
        Usuario currentUser = usuariosService.buscar(usuario.getUsuario().getUuid());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(senhaAtual, currentUser.getSenha())) {
            currentUser.setSenha(senha);
            if (usuariosService.salvar(currentUser)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
