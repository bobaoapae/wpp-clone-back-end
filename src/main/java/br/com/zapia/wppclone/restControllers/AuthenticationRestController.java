package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.authentication.UsuarioAuthentication;
import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.payloads.LoginRequest;
import br.com.zapia.wppclone.payloads.LoginResponse;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
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
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;

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
        loginResponse.setToken(jwt);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/resetDatabase")
    public ResponseEntity<?> resetDatabase() {
        for (Usuario usuario : usuariosService.listar()) {
            usuariosService.remover(usuario);
            whatsAppCloneService.finalizarInstanciaDoUsuarioSeEstiverAtiva(usuario);
        }
        for (Permissao permissao : permissoesService.listar()) {
            permissoesService.remover(permissao);
        }
        permissoesService.salvar(new Permissao("ROLE_USER"));
        permissoesService.salvar(new Permissao("ROLE_ADMIN"));
        permissoesService.salvar(new Permissao("ROLE_SUPER_ADMIN"));
        PasswordGenerator gen = new PasswordGenerator();
        CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
        CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
        lowerCaseRule.setNumberOfCharacters(2);

        CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
        CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
        upperCaseRule.setNumberOfCharacters(2);

        CharacterData digitChars = EnglishCharacterData.Digit;
        CharacterRule digitRule = new CharacterRule(digitChars);
        digitRule.setNumberOfCharacters(2);
        CharacterRule splCharRule = new CharacterRule(EnglishCharacterData.Special);
        splCharRule.setNumberOfCharacters(2);

        String password = gen.generatePassword(10, splCharRule, lowerCaseRule,
                upperCaseRule, digitRule);
        Usuario usuario = new Usuario();
        usuario.setLogin("admin");
        usuario.setSenha(password);
        usuario.setNome("Administrador");
        usuario.setPermissao(permissoesService.buscarPermissaoPorNome("ROLE_SUPER_ADMIN"));
        usuariosService.salvar(usuario);
        return ResponseEntity.ok().body(password);
    }
}
