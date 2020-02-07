package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.authentication.JwtTokenProvider;
import br.com.zapia.wppclone.authentication.UsuarioAuthentication;
import br.com.zapia.wppclone.modelo.dto.UsuarioResponseDTO;
import br.com.zapia.wppclone.payloads.LoginRequest;
import br.com.zapia.wppclone.payloads.LoginResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationRestController {

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
}
