package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import br.com.zapia.catarin.modelo.Usuario;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class UsuarioScoped implements Scope {

    private static final Logger log = Logger.getLogger(Usuario.class.getName());

    private final Map<String, Object> scopes = new ConcurrentHashMap<>();
    private final ReentrantLock readWriteLock = new ReentrantLock();


    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {
        try {
            log.info("UsuarioScoped -> get");
            readWriteLock.lock();
            log.info("UsuarioScoped -> lock");
            if (!scopes.containsKey(getConversationId() + s)) {
                scopes.put(getConversationId() + s, objectFactory.getObject());
            }
        } finally {
            readWriteLock.unlock();
            log.info("UsuarioScoped -> unlock");
        }
        return scopes.get(getConversationId() + s);
    }

    @Override
    public Object remove(String s) {
        try {
            log.info("UsuarioScoped -> remove");
            readWriteLock.lock();
            log.info("UsuarioScoped -> lock");
            return scopes.remove(getConversationId() + s);
        } finally {
            readWriteLock.unlock();
            log.info("UsuarioScoped -> unlock");
        }
    }

    @Override
    public void registerDestructionCallback(String s, Runnable runnable) {

    }

    @Override
    public Object resolveContextualObject(String s) {
        return "USUARIOSCOPED".equals(s);
    }

    @Override
    public String getConversationId() {
        return UsuarioScopedContext.getUsuario().getUuid().toString();
    }
}
