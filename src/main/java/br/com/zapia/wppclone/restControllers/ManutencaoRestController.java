package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance")
public class ManutencaoRestController {

    @Autowired
    private PermissoesService permissoesService;
    @Autowired
    private UsuariosService usuariosService;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;
    @Value("${securePas}")
    private String securePass;

    @PostMapping("/resetDatabase")
    public ResponseEntity<?> resetDatabase(@RequestParam("securePass") String securePass) {
        if (this.securePass.equals(securePass)) {
            for (Usuario usuario : usuariosService.listar()) {
                usuariosService.remover(usuario);
                whatsAppCloneService.finalizarInstanciaDoUsuarioSeEstiverAtiva(usuario);
            }
            for (Permissao permissao : permissoesService.listar()) {
                permissoesService.remover(permissao);
            }
            permissoesService.salvar(new Permissao("ROLE_OPERATOR"));
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

            String password = gen.generatePassword(20, splCharRule, lowerCaseRule,
                    upperCaseRule, digitRule);
            Usuario usuario = new Usuario();
            usuario.setLogin("admin");
            usuario.setSenha(password);
            usuario.setNome("Administrador");
            usuario.setPermissao(permissoesService.buscarPermissaoPorNome("ROLE_SUPER_ADMIN"));
            usuariosService.salvar(usuario);
            return ResponseEntity.ok().body(password);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
