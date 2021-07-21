package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.dto.*;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.TrocasDeNumerosService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
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
    private TrocasDeNumerosService trocasDeNumerosService;
    @Autowired
    private PermissoesService permissoesService;
    @Autowired
    private UsuarioPrincipalAutoWired usuario;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @PostMapping
    public ResponseEntity<?> criarNovoUsuario(@DTO(UsuarioCreateDTO.class) Usuario usuario) {
        usuario.setUsuarioPai(this.usuario.getUsuario());
        usuario.setPermissao(permissoesService.buscarPermissaoPorNome("ROLE_USER"));
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioBasicResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN"})
    @PutMapping
    public ResponseEntity<?> atualizarUsuario(@DTO(UsuarioUpdateDTO.class) Usuario usuario) {
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioBasicResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN"})
    @DeleteMapping("/{uuid}")
    public ResponseEntity<?> deletarUsuario(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            if (usuariosService.remover(usuario)) {
                whatsAppCloneService.finalizarInstanciaDoUsuarioSeEstiverAtiva(usuario);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @PostMapping("/desativar/{uuid}")
    public ResponseEntity<?> desativarUsuario(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            if (this.usuario.isSuperAdmin() || usuario.getUsuarioPai().equals(this.usuario.getUsuario())) {
                if (usuariosService.desativar(usuario)) {
                    whatsAppCloneService.finalizarInstanciaDoUsuarioSeEstiverAtiva(usuario);
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

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @PostMapping("/ativar/{uuid}")
    public ResponseEntity<?> ativarUsuario(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            if (this.usuario.isSuperAdmin() || usuario.getUsuarioPai().equals(this.usuario.getUsuario())) {
                if (usuariosService.ativar(usuario)) {
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

    @GetMapping("/{uuid}")
    public ResponseEntity<?> verUsuario(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/self")
    public ResponseEntity<?> verUsuarioLogado() {
        return ResponseEntity.ok(modelMapper.map(usuariosService.buscar(usuario.getUsuario().getUuid()), UsuarioBasicResponseDTO.class));
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        List<Usuario> listar;
        if (usuario.isSuperAdmin()) {
            listar = usuariosService.listar();
        } else {
            listar = usuariosService.listarUsuariosFilhos(usuario.getUsuario());
        }
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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("A senha atual informada não é válida.");
        }
    }

    @Secured({"ROLE_SUPER_ADMIN"})
    @GetMapping("/resetarSenha/{uuid}")
    public ResponseEntity<?> resetarSenha(@PathVariable("uuid") String uuid) {
        Usuario usuario = usuariosService.buscar(UUID.fromString(uuid));
        if (usuario != null) {
            String novaSenha = Util.gerarSenha(10, false);
            usuario.setSenha(novaSenha);
            if (usuariosService.salvar(usuario)) {
                return ResponseEntity.ok().body(novaSenha);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
