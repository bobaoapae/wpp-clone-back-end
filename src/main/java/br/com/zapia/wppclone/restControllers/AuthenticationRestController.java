package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.authentication.UsuarioAuthentication;
import br.com.zapia.wppclone.modelo.TrocaDeNumero;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.dto.TrocaDeNumeroDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;
import br.com.zapia.wppclone.payloads.LoginRequest;
import br.com.zapia.wppclone.payloads.LoginResponse;
import br.com.zapia.wppclone.servicos.TrocasDeNumerosService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import modelo.Chat;
import modelo.EstadoDriver;
import modelo.Message;
import modelo.MessageBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationRestController {

    @Autowired
    private UsuariosService usuariosService;
    @Autowired
    private TrocasDeNumerosService trocasDeNumerosService;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @ModelAttribute LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getLogin(),
                        loginRequest.getSenha()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UsuarioAuthentication userPrincipal = (UsuarioAuthentication) authentication.getPrincipal();

        String jwt = tokenProvider.generateToken(userPrincipal);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUsuario(modelMapper.map(userPrincipal.getUsuario(), UsuarioResponseDTO.class));
        loginResponse.setToken(jwt);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/changeNumber/{uuid}")
    public ResponseEntity<?> changeNumberInfo(@PathVariable("uuid") String uuid) {
        TrocaDeNumero trocaDeNumero = trocasDeNumerosService.buscar(UUID.fromString(uuid));
        if (trocaDeNumero != null) {
            if (trocaDeNumero.getCriadoEm().plusHours(2).isAfter(LocalDateTime.now()) && trocaDeNumero.isAtivo()) {
                return ResponseEntity.ok(modelMapper.map(trocaDeNumero, TrocaDeNumeroDTO.class));
            } else {
                return ResponseEntity.badRequest().body("Token expirado, realize uma nova solicitação.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/changeNumber/{uuid}")
    public ResponseEntity<?> changeNumberConfirm(@PathVariable("uuid") String uuid) {
        TrocaDeNumero trocaDeNumero = trocasDeNumerosService.buscar(UUID.fromString(uuid));
        if (trocaDeNumero != null) {
            if (trocaDeNumero.getCriadoEm().plusHours(2).isAfter(LocalDateTime.now()) && trocaDeNumero.isAtivo()) {
                if (usuariosService.efetivarTrocaDeNumero(trocaDeNumero)) {
                    return ResponseEntity.ok().build();
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return ResponseEntity.badRequest().body("Token expirado, realize uma nova solicitação.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/resetPassword")
    public ResponseEntity<?> resetarSenha(@RequestParam("login") String login) {
        Usuario usuario = usuariosService.buscarUsuarioPorLogin(login);
        if (usuario != null) {
            String novaSenha = Util.gerarSenha(10, false);
            WhatsAppClone instanciaGeral = whatsAppCloneService.getInstanciaGeral();
            if (instanciaGeral != null && instanciaGeral.getDriver().getEstadoDriver() == EstadoDriver.LOGGED) {
                Chat chat = instanciaGeral.getDriver().getFunctions().getChatByNumber(usuario.getTelefone()).join();
                if (chat != null) {
                    String oldHash = usuario.getSenha();
                    usuario.setSenha(novaSenha);
                    if (usuariosService.salvar(usuario)) {
                        MessageBuilder messageBuilder = new MessageBuilder();
                        messageBuilder.text("Olá ").textBold(usuario.getNome()).text(".")
                                .newLine()
                                .newLine()
                                .text("Sua nova senha de acesso é: ").newLine().newLine().textBold(novaSenha);
                        Message sendMessage = chat.sendWebSite("https://wpp.zapia.com.br/login", messageBuilder.build()).join();
                        if (sendMessage != null) {
                            return ResponseEntity.ok(modelMapper.map(usuario, UsuarioResponseDTO.class));
                        } else {
                            usuario.setSenha(oldHash);
                            usuario.setUpdateSenha(false);
                            usuariosService.salvar(usuario);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao enviar a mensagem contendo a nova senha, tente novamente mais tarde.");
                        }
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao salvar usuário, tente novamente mais tarde.");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não foi possível encontrar uma conta do WhatsApp com o número cadastrado para esse usuário.");
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao enviar nova senha por WhatsApp, tente novamente mais tarde.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
