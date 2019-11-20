package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class UsuarioContextPoolExecutor extends ThreadPoolTaskExecutor {

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(new UsuarioContextCallable<>(task, UsuarioScopedContext.getUsuario()));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return super.submitListenable(new UsuarioContextCallable<>(task, UsuarioScopedContext.getUsuario()));
    }

    @Override
    public void execute(Runnable task) {
        super.execute(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()));
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        super.execute(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()));
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        return super.submitListenable(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()));
    }

}
