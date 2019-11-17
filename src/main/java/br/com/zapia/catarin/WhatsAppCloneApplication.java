package br.com.zapia.catarin;

import br.com.zapia.catarin.authentication.UsuarioScopedBeanFactoryPostProcessor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class WhatsAppCloneApplication implements AsyncConfigurer {

    public static void main(String[] args) {
        SecurityContextHolder.setStrategyName("MODE_THREADLOCAL");
        SpringApplicationBuilder builder = new SpringApplicationBuilder(WhatsAppCloneApplication.class);
        builder.headless(false).run(args);
    }

    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new UsuarioScopedBeanFactoryPostProcessor();
    }

    @Override
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(50);
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
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Bean
    public Executor taskExecutor() {
        return getAsyncExecutor();
    }

}
