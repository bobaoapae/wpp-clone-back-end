package br.com.zapia.catarin.restControllers;

import br.com.zapia.catarin.modelo.Entidade;
import br.com.zapia.catarin.servicos.CRUDService;
import br.com.zapia.catarin.utils.RequestBodyInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public abstract class CRUDRest<T extends Entidade> {

    @PostMapping(value = "/salvar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> salvar(@RequestBody T obj) {
        try {
            if (getService().salvar(obj)) {
                return ResponseEntity.ok().body(obj);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new RequestBodyInfo(e));
        }
    }

    @DeleteMapping(value = "/remover/{uuid}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> remover(@PathVariable String uuid) {
        try {
            T entidade = getService().buscar(UUID.fromString(uuid));
            if (entidade != null) {
                if (getService().remover(entidade)) {
                    return ResponseEntity.ok().build();
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new RequestBodyInfo(e));
        }
    }

    @GetMapping(value = "/ver/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ver(@PathVariable String uuid) {
        try {
            T entidade = getService().buscar(UUID.fromString(uuid));
            if (entidade != null) {
                return ResponseEntity.ok(entidade);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new RequestBodyInfo(e));
        }
    }

    @GetMapping(value = "/listar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(getService().listar());
    }

    abstract CRUDService<T> getService();
}
