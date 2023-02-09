package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Permissao_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;


@Repository
public class PermissoesRepository extends CRUDRepository<Permissao> {

    public PermissoesRepository() {
        super(Permissao.class);
    }

    public Permissao buscarPermissaoPorNome(String nome) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<Permissao> criteriaQuery = getCriteriaQuery();
        Root<Permissao> root = criteriaQuery.from(Permissao.class);
        criteriaQuery.select(root).where(criteriaBuilder.equal(root.get(Permissao_.permissao), nome));
        return DataAccessUtils.singleResult(getEm().createQuery(criteriaQuery).getResultList());
    }
}
