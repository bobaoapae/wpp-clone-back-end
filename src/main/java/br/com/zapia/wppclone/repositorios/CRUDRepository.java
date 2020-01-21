package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.Entidade;
import br.com.zapia.wppclone.modelo.Entidade_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class CRUDRepository<T extends Entidade> {

    private final Logger logger;
    private final Class<T> classeEntidade;
    @PersistenceContext
    private EntityManager em;

    public CRUDRepository(Class<T> classeEntidade) {
        this.classeEntidade = classeEntidade;
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Transactional
    public boolean salvar(T entidade) {
        try {
            em.persist(entidade);
            return true;
        } catch (Exception e) {
            logger.error("Erro ao Salvar", e);
            return false;
        }
    }

    @Transactional
    public boolean remover(T entidade) {
        try {
            em.refresh(entidade);
            em.remove(entidade);
            return true;
        } catch (Exception e) {
            logger.error("Erro ao Excluir", e);
            return false;
        }
    }

    @Transactional
    public boolean remover(UUID uuid) {
        try {
            return remover(buscar(uuid));
        } catch (Exception e) {
            logger.error("Erro ao Excluir", e);
            return false;
        }
    }

    @Transactional
    public boolean desativar(T entidade) {
        try {
            entidade.setAtivo(false);
            return salvar(entidade);
        } catch (Exception e) {
            logger.error("Erro ao Desativar", e);
            return false;
        }
    }

    @Transactional
    public boolean desativar(UUID uuid) {
        try {
            T entidade = buscar(uuid);
            return desativar(entidade);
        } catch (Exception e) {
            logger.error("Erro ao Desativar", e);
            return false;
        }
    }

    @Transactional
    public boolean ativar(T entidade) {
        try {
            entidade.setAtivo(true);
            return salvar(entidade);
        } catch (Exception e) {
            logger.error("Erro ao Ativar", e);
            return false;
        }
    }

    @Transactional
    public boolean ativar(UUID uuid) {
        try {
            T entidade = buscar(uuid);
            return ativar(entidade);
        } catch (Exception e) {
            logger.error("Erro ao Ativar", e);
            return false;
        }
    }

    @Transactional
    public T buscar(UUID uuid) {
        try {
            return em.find(classeEntidade, uuid);
        } catch (Exception e) {
            logger.error("Erro ao Excluir", e);
            return null;
        }
    }

    @Transactional
    public List<T> listar() {
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<T> query = builder.createQuery(classeEntidade);
            Root<T> root = query.from(classeEntidade);
            query.select(root).where(builder.equal(root.get(Entidade_.ativo), true));
            return em.createQuery(query).getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public CriteriaQuery<T> getCriteriaQuery() {
        return getCriteriaBuilder().createQuery(classeEntidade);
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return getEm().getCriteriaBuilder();
    }

    public EntityManager getEm() {
        return em;
    }
}
