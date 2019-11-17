package br.com.zapia.catarin;

import br.com.zapia.catarin.authentication.UsuarioScopedBeanFactoryPostProcessor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class WhatsAppCloneApplication {

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(WhatsAppCloneApplication.class);
        builder.headless(false).run(args);
    }

    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new UsuarioScopedBeanFactoryPostProcessor();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
