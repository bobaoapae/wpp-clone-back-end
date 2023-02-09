package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty;
import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;
import br.com.zapia.wppclone.repositorios.CRUDRepository;
import br.com.zapia.wppclone.repositorios.WhatsAppObjectWithPropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WhatsAppObjectWithPropertyService extends CRUDService<WhatsAppObjectWithIdProperty> {

    @Autowired
    private WhatsAppObjectWithPropertyRepository whatsAppObjectWithPropertyRepository;

    public List<WhatsAppObjectWithIdProperty> buscarPropriedades(WhatsAppObjectWithIdType type, String id) {
        return whatsAppObjectWithPropertyRepository.buscarPropriedades(type, id);
    }

    public WhatsAppObjectWithIdProperty buscarPropriedade(WhatsAppObjectWithIdType type, String id, String key) {
        return whatsAppObjectWithPropertyRepository.buscarPropriedade(type, id, key);
    }

    public List<String> buscarWhatsAppIdsComPropriedade(WhatsAppObjectWithIdType type, String key, String value) {
        return whatsAppObjectWithPropertyRepository.buscarWhatsAppIdsComPropriedade(type, key, value);
    }

    public boolean alterarValor(UUID uuid, String value) {
        WhatsAppObjectWithIdProperty property = whatsAppObjectWithPropertyRepository.buscar(uuid);
        if (property != null) {
            property.setValue(value);
            return whatsAppObjectWithPropertyRepository.salvar(property);
        }

        return false;
    }

    @Override
    public CRUDRepository<WhatsAppObjectWithIdProperty> getRepository() {
        return whatsAppObjectWithPropertyRepository;
    }
}
