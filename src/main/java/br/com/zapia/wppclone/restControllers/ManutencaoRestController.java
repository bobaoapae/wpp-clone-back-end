package br.com.zapia.wppclone.restControllers;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.PermissoesService;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.utils.Util;
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
            String password = Util.generateRandomString(20, true);
            Usuario usuario = new Usuario();
            usuario.setLogin("admin");
            usuario.setSenha(password);
            usuario.setNome("Administrador");
            usuario.setTelefone("999999999");
            usuario.setPermissao(permissoesService.buscarPermissaoPorNome("ROLE_SUPER_ADMIN"));
            usuariosService.salvar(usuario);
            return ResponseEntity.ok().body(password);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
