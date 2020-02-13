package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.wppclone.modelo.TrocaDeNumero;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.dto.DTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioCreateDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioUpdateDTO;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.TrocasDeNumerosService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import modelo.Chat;
import modelo.EstadoDriver;
import modelo.MessageBuilder;
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
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN"})
    @PutMapping
    public ResponseEntity<?> atualizarUsuario(@DTO(UsuarioUpdateDTO.class) Usuario usuario) {
        if (usuariosService.salvar(usuario)) {
            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Secured({"ROLE_SUPER_ADMIN"})
    @PutMapping("/alterarNumero")
    public ResponseEntity<?> alterarNumero(@RequestParam("telefone") String telefone) {
        WhatsAppClone instanciaGeral = whatsAppCloneService.getInstanciaGeral();
        if (instanciaGeral != null && instanciaGeral.getDriver().getEstadoDriver() == EstadoDriver.LOGGED) {
            Chat novoNumero = instanciaGeral.getDriver().getFunctions().getChatByNumber(telefone).join();
            if (novoNumero != null) {
                TrocaDeNumero trocaDeNumero = new TrocaDeNumero();
                trocaDeNumero.setUsuario(usuario.getUsuario());
                trocaDeNumero.setNovoNumero(telefone);
                if (trocasDeNumerosService.salvar(trocaDeNumero)) {
                    MessageBuilder messageBuilder = new MessageBuilder();
                    messageBuilder.text("Olá ").textBold(usuario.getUsuario().getNome()).text(".")
                            .newLine()
                            .newLine()
                            .text("Clique aqui para confirmar a troca de número da sua conta.").newLine().newLine();
                    if (usuario.getUsuario().getTelefone().equals("000000000")) {
                        novoNumero.sendWebSite("https://wpp.zapia.com.br/confirmchangenumber?token=" + trocaDeNumero.getUuid(), messageBuilder.build()).join();
                        return ResponseEntity.ok().build();
                    } else {
                        Chat numeroAtual = instanciaGeral.getDriver().getFunctions().getChatByNumber(usuario.getUsuario().getTelefone()).join();
                        if (numeroAtual != null) {
                            numeroAtual.sendWebSite("https://wpp.zapia.com.br/confirmchangenumber?token=" + trocaDeNumero.getUuid(), messageBuilder.build()).join();
                            return ResponseEntity.ok().build();
                        } else {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não foi possível enviar a mensagem de confirmação para o número atual, tente novamente mais tarde");
                        }
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não foi possível encontrar o número informado no WhatsApp, verifique e tente novamente.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao enviar nova senha por WhatsApp, tente novamente mais tarde.");
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
}
