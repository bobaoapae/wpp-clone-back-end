package br.com.zapia.catarin.authentication.scopeInjectionHandler;

import br.com.zapia.catarin.modelo.Usuario;

import java.util.concurrent.*;

public class UsuarioContextThreadPoolScheduler extends ScheduledThreadPoolExecutor {

    private Usuario usuario;

    public UsuarioContextThreadPoolScheduler(Usuario usuario, int corePoolSize) {
        super(corePoolSize);
        this.usuario = usuario;
    }

    public UsuarioContextThreadPoolScheduler(Usuario usuario, int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
        this.usuario = usuario;
    }

    public UsuarioContextThreadPoolScheduler(Usuario usuario, int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
        this.usuario = usuario;
    }

    public UsuarioContextThreadPoolScheduler(Usuario usuario, int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
        this.usuario = usuario;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(new UsuarioContextCallable<>(task, usuario));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(new UsuarioContextRunnable(task, usuario));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(new UsuarioContextRunnable(task, usuario), result);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(new UsuarioContextRunnable(command, usuario), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(new UsuarioContextCallable<>(callable, usuario), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(new UsuarioContextRunnable(command, usuario), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(new UsuarioContextRunnable(command, usuario), initialDelay, delay, unit);
    }
}
