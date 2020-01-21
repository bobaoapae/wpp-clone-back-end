package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.LoginRequest;
import br.com.zapia.wppclone.payloads.LoginResponse;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationRestController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private PermissoesService permissoesService;
    @Autowired
    private UsuariosService usuariosService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @ModelAttribute LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getLogin(),
                        loginRequest.getSenha()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(jwt);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/resetDatabase")
    public ResponseEntity<?> resetDatabase() {
        for (Usuario usuario : usuariosService.listar()) {
            usuariosService.remover(usuario);
        }
        for (Permissao permissao : permissoesService.listar()) {
            permissoesService.remover(permissao);
        }
        permissoesService.salvar(new Permissao("ROLE_OPERADOR"));
        permissoesService.salvar(new Permissao("ROLE_ADMIN"));
        permissoesService.salvar(new Permissao("ROLE_SUPER_ADMIN"));
        Usuario usuario = new Usuario();
        usuario.setLogin("joao");
        usuario.setSenha("joao0123@");
        usuario.setNome("João Vitor Borges");
        usuario.setPermissao(permissoesService.buscarPermissaoPorNome("ROLE_SUPER_ADMIN"));
        usuariosService.salvar(usuario);
        return ResponseEntity.ok().build();
    }
}
