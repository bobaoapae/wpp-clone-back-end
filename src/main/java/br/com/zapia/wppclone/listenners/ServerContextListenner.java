package br.com.zapia.wppclone.listenners;

import br.com.zapia.wppclone.authentication.scopeInjectionHandler.UsuarioScopedContext;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.servicos.UsuariosService;
import br.com.zapia.wppclone.servicos.WhatsAppCloneService;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

@Component
public class ServerContextListenner implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ServerContextListenner.class);

    @Autowired
    private UsuariosService usuariosService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private WhatsAppCloneService whatsAppCloneService;
    @Value("${loginWhatsAppGeral}")
    private String loginWhatsAppGeral;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Usuario usuario = usuariosService.buscarUsuarioPorLogin(loginWhatsAppGeral);
        if (usuario != null) {
            UsuarioScopedContext.setUsuario(usuario);
            WhatsAppClone whatsAppClone = applicationContext.getBean(WhatsAppClone.class);
            whatsAppCloneService.setInstanciaGeral(whatsAppClone);
        } else {
            logger.error("Usuario para o WhatsApp Geral não encontrado");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
