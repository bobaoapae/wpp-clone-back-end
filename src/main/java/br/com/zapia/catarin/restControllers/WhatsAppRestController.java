package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.whatsApp.CatarinWhatsApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsApp")
public class WhatsAppRestController {

    @Autowired
    private CatarinWhatsApp catarinWhatsApp;

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_OPERADOR"})
    @GetMapping("/estadoWhats")
    public ResponseEntity<?> estadoWhats() {
        Map<String, Object> dados = new HashMap<>();
        dados.put("status", catarinWhatsApp.getDriver().getEstadoDriver().name());
        return ResponseEntity.ok(dados);
    }
}
