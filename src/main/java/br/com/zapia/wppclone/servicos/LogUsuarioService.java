package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.LogUsuario;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.repositorios.CRUDRepository;
import br.com.zapia.wppclone.repositorios.LogUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogUsuarioService extends CRUDService<LogUsuario> {

    @Autowired
    private LogUsuarioRepository logUsuarioRepository;

    public boolean registrarLog(Usuario usuario, String log) {
        var logRegistry = new LogUsuario();
        logRegistry.setUsuario(usuario);
        logRegistry.setLog(log);
        return salvar(logRegistry);
    }

    @Override
    public CRUDRepository<LogUsuario> getRepository() {
        return logUsuarioRepository;
    }
}
