package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OperadoresService {

    private static final Logger log = LoggerFactory.getLogger(OperadoresService.class);
    private final Map<Usuario, Map<Usuario, List<WebSocketSession>>> sessoesAtivas;
    private final ReentrantLock lock;

    public OperadoresService() {
        sessoesAtivas = new LinkedHashMap<>();
        lock = new ReentrantLock();
    }

    public void adicionarSessao(WebSocketSession session) {
        try {
            lock.lock();
            Usuario usuario = (Usuario) session.getAttributes().get("usuario");
            if (!sessoesAtivas.containsKey(usuario.getUsuarioResponsavelPelaInstancia())) {
                sessoesAtivas.put(usuario.getUsuarioResponsavelPelaInstancia(), new LinkedHashMap<>());
            }
            if (!sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).containsKey(usuario)) {
                sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).put(usuario, new ArrayList<>());
            }
            sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).get(usuario).add(session);
        } catch (Exception e) {
            log.error("Adicionar Instancia", e);
        } finally {
            lock.unlock();
        }
    }

    public void removerSessao(WebSocketSession session) {
        try {
            lock.lock();
            Usuario usuario = (Usuario) session.getAttributes().get("usuario");
            if (sessoesAtivas.containsKey(usuario.getUsuarioResponsavelPelaInstancia()) && sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).containsKey(usuario)) {
                sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).get(usuario).remove(session);
            }
        } catch (Exception e) {
            log.error("Remover Instancia", e);
        } finally {
            lock.unlock();
        }
    }

    public boolean finalizarSessoesParaOperadorSeEstiveremAtivas(Usuario usuario) {
        try {
            lock.lock();
            if (sessoesAtivas.containsKey(usuario.getUsuarioResponsavelPelaInstancia()) && sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).containsKey(usuario)) {
                sessoesAtivas.get(usuario.getUsuarioResponsavelPelaInstancia()).get(usuario).forEach(webSocketSession -> {
                    try {
                        webSocketSession.close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (IOException e) {
                        log.error("Finalizar Sessões do Operador", e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Finalizar Instancia do Usuário", e);
        } finally {
            lock.unlock();
        }
        return false;
    }
}
