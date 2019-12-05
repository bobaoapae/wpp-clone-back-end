package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import br.com.zapia.catarin.modelo.Usuario;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioScoped implements Scope {

    private static final Logger log = Logger.getLogger(Usuario.class.getName());

    private final Map<String, Object> scopes = new ConcurrentHashMap<>();
    private final static ReentrantLock lock = new ReentrantLock();


    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {
        try {
            log.info("UsuarioScoped -> get");
            lock.lock();
            log.info("UsuarioScoped -> lock");
            if (!scopes.containsKey(getConversationId() + s)) {
                scopes.put(getConversationId() + s, objectFactory.getObject());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "UsuarioScoped", e);
            throw new IllegalStateException();
        } finally {
            lock.unlock();
            log.info("UsuarioScoped -> unlock");
        }
        return scopes.get(getConversationId() + s);
    }

    @Override
    public Object remove(String s) {
        try {
            log.info("UsuarioScoped -> remove");
            lock.lock();
            log.info("UsuarioScoped -> lock");
            return scopes.remove(getConversationId() + s);
        } catch (Exception e) {
            log.log(Level.SEVERE, "UsuarioScoped", e);
        } finally {
            lock.unlock();
            log.info("UsuarioScoped -> unlock");
        }
        return null;
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
