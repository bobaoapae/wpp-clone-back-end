package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Permissao_;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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
