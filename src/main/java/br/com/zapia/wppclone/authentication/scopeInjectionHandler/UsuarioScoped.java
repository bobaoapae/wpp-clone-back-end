package br.com.zapia.wppclone.authentication.scopeInjectionHandler;

import br.com.zapia.wppclone.modelo.Usuario;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioScoped implements Scope {

    private static final Logger log = Logger.getLogger(Usuario.class.getName());

    private final static Map<String, Object> scopes = new HashMap<>();
    private final static ReentrantLock lock = new ReentrantLock();


    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {
        try {
            lock.lock();
            if (!scopes.containsKey(getConversationId() + s)) {
                scopes.put(getConversationId() + s, objectFactory.getObject());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "UsuarioScoped: " + s, e);
            throw new IllegalStateException();
        } finally {
            lock.unlock();
        }
        return scopes.get(getConversationId() + s);
    }

    @Override
    public Object remove(String s) {
        try {
            lock.lock();
            return scopes.remove(getConversationId() + s);
        } catch (Exception e) {
            log.log(Level.SEVERE, "UsuarioScoped", e);
        } finally {
            lock.unlock();
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
