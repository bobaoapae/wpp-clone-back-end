package br.com.zapia.wppclone.authentication.scopeInjectionHandler;

import br.com.zapia.wppclone.modelo.Usuario;

import java.util.concurrent.*;

public class UsuarioContextThreadPoolExecutor extends ThreadPoolExecutor {

    private Usuario usuario;

    public UsuarioContextThreadPoolExecutor(Usuario usuario, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.usuario = usuario;
    }

    public UsuarioContextThreadPoolExecutor(Usuario usuario, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.usuario = usuario;
    }

    public UsuarioContextThreadPoolExecutor(Usuario usuario, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        this.usuario = usuario;
    }

    public UsuarioContextThreadPoolExecutor(Usuario usuario, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
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


}
