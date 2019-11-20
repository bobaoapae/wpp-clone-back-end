package br.com.zapia.catarin;

import br.com.zapia.catarin.authentication.scopeInjectionHandler.UsuarioContextPoolExecutor;
import br.com.zapia.catarin.authentication.scopeInjectionHandler.UsuarioContextTaskScheduler;
import br.com.zapia.catarin.authentication.scopeInjectionHandler.UsuarioScopedBeanFactoryPostProcessor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class WhatsAppCloneApplication implements AsyncConfigurer, SchedulingConfigurer {

    public static void main(String[] args) {
        SecurityContextHolder.setStrategyName("MODE_INHERITABLETHREADLOCAL");
        SpringApplicationBuilder builder = new SpringApplicationBuilder(WhatsAppCloneApplication.class);
        builder.headless(false).run(args);
    }

    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new UsuarioScopedBeanFactoryPostProcessor();
    }

    @Override
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new UsuarioContextPoolExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(1000);
        executor.setQueueCapacity(200);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Wpp-Async-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public DelegatingSecurityContextAsyncTaskExecutor taskExecutor(ThreadPoolTaskExecutor delegate) {
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskScheduler executor = new UsuarioContextTaskScheduler();
        executor.setPoolSize(1000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Wpp-Async-Scheduler");
        executor.initialize();
        return executor;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }
}
