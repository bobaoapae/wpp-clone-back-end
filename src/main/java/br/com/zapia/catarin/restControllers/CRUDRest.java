package br.com.zapia.catarin.restControllers;


import br.com.zapia.catarin.authentication.UsuarioPrincipalAutoWired;
import br.com.zapia.catarin.modelo.Entidade;
import br.com.zapia.catarin.modelo.Usuario;
import br.com.zapia.catarin.servicos.CRUDService;
import br.com.zapia.catarin.utils.NullAwareBeanUtilsBean;
import br.com.zapia.catarin.utils.ResponseError;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

@RestController
public abstract class CRUDRest<T extends Entidade> {

    @Autowired
    private UsuarioPrincipalAutoWired usuarioPrincipalAutoWired;
    private Class<T> classe;

    public CRUDRest() {
    }

    public CRUDRest(Class<T> classe) {
        this.classe = classe;
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody T obj) {
        try {
            T objBanco;
            if (obj.getUuid() != null) {
                objBanco = getService().buscar(obj.getUuid());
                new NullAwareBeanUtilsBean().copyProperties(objBanco, obj);
            } else {
                objBanco = obj;
            }
            if (getService().salvar(objBanco)) {
                return ResponseEntity.ok().body(obj);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseError(e));
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @DeleteMapping(value = "/{uuid}")
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseError(e.getMessage()));
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @GetMapping(value = "/{uuid}")
    public ResponseEntity<?> ver(@PathVariable String uuid) {
        try {
            T entidade = getService().buscar(UUID.fromString(uuid));
            if (entidade != null) {
                return ResponseEntity.ok(entidade);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseError(e.getMessage()));
        }
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(getService().listar());
    }

    @Secured({"ROLE_SUPER_ADMIN", "ROLE_ADMIN"})
    @GetMapping("/modelo")
    public ResponseEntity<?> modelo() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        return ResponseEntity.ok(objectMapper.writeValueAsBytes(classe.getDeclaredConstructor().newInstance()));
    }

    abstract CRUDService<T> getService();

    public Usuario getCurrentUser() {
        return usuarioPrincipalAutoWired.getUsuario();
    }
}
