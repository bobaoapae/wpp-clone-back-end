package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.authentication.UsuarioAuthentication;
import br.com.zapia.wppclone.modelo.TrocaDeNumero;
import br.com.zapia.wppclone.modelo.dto.TrocaDeNumeroDTO;
import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;
import br.com.zapia.wppclone.payloads.LoginRequest;
import br.com.zapia.wppclone.payloads.LoginResponse;
import br.com.zapia.wppclone.servicos.TrocasDeNumerosService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
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
}
