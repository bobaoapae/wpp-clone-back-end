package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty;
import br.com.zapia.wppclone.modelo.WhatsAppObjectWithIdProperty_;
import br.com.zapia.wppclone.modelo.enums.WhatsAppObjectWithIdType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WhatsAppObjectWithPropertyRepository extends CRUDRepository<WhatsAppObjectWithIdProperty> {

    public WhatsAppObjectWithPropertyRepository() {
        super(WhatsAppObjectWithIdProperty.class);
    }

    public List<WhatsAppObjectWithIdProperty> buscarPropriedades(WhatsAppObjectWithIdType type, String id) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<WhatsAppObjectWithIdProperty> criteriaQuery = getCriteriaQuery();
        Root<WhatsAppObjectWithIdProperty> root = criteriaQuery.from(WhatsAppObjectWithIdProperty.class);
        criteriaQuery.select(root).where(criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.WHATS_APP_ID), id), criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.TYPE), type));
        return getEm().createQuery(criteriaQuery).getResultList();
    }

    public WhatsAppObjectWithIdProperty buscarPropriedade(WhatsAppObjectWithIdType type, String id, String key) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<WhatsAppObjectWithIdProperty> criteriaQuery = getCriteriaQuery();
        Root<WhatsAppObjectWithIdProperty> root = criteriaQuery.from(WhatsAppObjectWithIdProperty.class);
        criteriaQuery.select(root).where(
                criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.WHATS_APP_ID), id),
                criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.TYPE), type),
                criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.KEY), key));
        return DataAccessUtils.singleResult(getEm().createQuery(criteriaQuery).getResultList());
    }

    public List<String> buscarWhatsAppIdsComPropriedade(WhatsAppObjectWithIdType type, String key, String value) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<WhatsAppObjectWithIdProperty> criteriaQuery = getCriteriaQuery();
        Root<WhatsAppObjectWithIdProperty> root = criteriaQuery.from(WhatsAppObjectWithIdProperty.class);
        criteriaQuery.select(root).where(
                criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.TYPE), type),
                criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.KEY), key),
                criteriaBuilder.equal(root.get(WhatsAppObjectWithIdProperty_.VALUE), value)
        );
        return getEm().createQuery(criteriaQuery).getResultList().stream().map(WhatsAppObjectWithIdProperty::getWhatsAppId).toList();
    }
}
