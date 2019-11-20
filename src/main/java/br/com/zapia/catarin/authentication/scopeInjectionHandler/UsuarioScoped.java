package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UsuarioScoped implements Scope {

    private final Map<String, Object> scopes = new ConcurrentHashMap<>();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();


    @Override
    public Object get(String s, ObjectFactory<?> objectFactory) {
        try {
            readWriteLock.writeLock().lock();
            if (!scopes.containsKey(getConversationId() + s)) {
                scopes.put(getConversationId() + s, objectFactory.getObject());
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return scopes.get(getConversationId() + s);
    }

    @Override
    public Object remove(String s) {
        return scopes.remove(getConversationId() + s);
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
