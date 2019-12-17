package br.com.zapia.wppclone.authentication.scopeInjectionHandler;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

public class UsuarioContextTaskScheduler extends ThreadPoolTaskScheduler {

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return super.schedule(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), trigger);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
        return super.schedule(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), startTime);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
        return super.scheduleAtFixedRate(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), startTime, period);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
        return super.scheduleAtFixedRate(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), period);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
        return super.scheduleWithFixedDelay(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), startTime, delay);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
        return super.scheduleWithFixedDelay(new UsuarioContextRunnable(task, UsuarioScopedContext.getUsuario()), delay);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(new UsuarioContextCallable<>(task, UsuarioScopedContext.getUsuario()));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return super.submitListenable(new UsuarioContextCallable<>(task, UsuarioScopedContext.getUsuario()));
    }
}
