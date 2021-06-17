package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.whatsApp.WhatsAppClone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class WhatsAppCloneService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloneService.class);
    private final Map<Usuario, WhatsAppClone> instanciasAtivas;
    private final ReentrantLock lock;
    private LocalDateTime lastWarningVersion;

    public WhatsAppCloneService() {
        instanciasAtivas = new LinkedHashMap<>();
        lock = new ReentrantLock();
    }

    public void adicionarInstancia(WhatsAppClone whatsAppClone) {
        try {
            lock.lock();
            instanciasAtivas.put(whatsAppClone.getUsuario().getUsuarioResponsavelPelaInstancia(), whatsAppClone);
        } catch (Exception e) {
            log.error("Adicionar Instancia", e);
        } finally {
            lock.unlock();
        }
    }

    public void removerInstancia(WhatsAppClone whatsAppClone) {
        try {
            lock.lock();
            instanciasAtivas.remove(whatsAppClone.getUsuario().getUsuarioResponsavelPelaInstancia());
        } catch (Exception e) {
            log.error("Remover Instancia", e);
        } finally {
            lock.unlock();
        }
    }

    public boolean finalizarInstanciaDoUsuarioSeEstiverAtiva(Usuario usuario) {
        try {
            lock.lock();
            WhatsAppClone whatsAppClone = instanciasAtivas.get(usuario.getUsuarioResponsavelPelaInstancia());
            if (whatsAppClone != null) {
                whatsAppClone.setForceShutdown(true);
                return true;
            }
        } catch (Exception e) {
            log.error("Finalizar Instancia do Usuário", e);
        } finally {
            lock.unlock();
        }
        return false;
    }

    public Map<Usuario, WhatsAppClone> getInstanciasAtivas() {
        try {
            lock.lock();
            return Collections.unmodifiableMap(new LinkedHashMap<>(instanciasAtivas));
        } catch (Exception e) {
            log.error("Get All Instancias", e);
        } finally {
            lock.unlock();
        }
        return null;
    }

    public boolean canSendWarningVersion(){
        return lastWarningVersion == null || lastWarningVersion.plusHours(2).isBefore(LocalDateTime.now());
    }

    public void setSendWarningVersion(){
        lastWarningVersion = LocalDateTime.now();
    }
}
