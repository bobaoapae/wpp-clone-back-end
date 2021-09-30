package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.LogUsuario;
import org.springframework.stereotype.Repository;

@Repository
public class LogUsuarioRepository extends CRUDRepository<LogUsuario> {

    public LogUsuarioRepository() {
        super(LogUsuario.class);
    }
}
