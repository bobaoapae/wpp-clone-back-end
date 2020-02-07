package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.modelo.Permissao_;
import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.modelo.Usuario_;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

@Repository
public class UsuariosRepository extends CRUDRepository<Usuario> {

    public UsuariosRepository() {
        super(Usuario.class);
    }

    public Usuario buscarUsuarioPorLogin(String login) {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<Usuario> query = getCriteriaQuery();
        Root<Usuario> root = query.from(Usuario.class);
        query.select(root).where(builder.equal(root.get(Usuario_.login), login));
        return DataAccessUtils.singleResult(getEm().createQuery(query).getResultList());
    }

    public List<Usuario> listarUsuariosFilhos(Usuario usuarioPai) {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<Usuario> query = getCriteriaQuery();
        Root<Usuario> root = query.from(Usuario.class);
        query.select(root).where(builder.equal(root.get(Usuario_.USUARIO_PAI), usuarioPai));
        return getEm().createQuery(query).getResultList();
    }

    public List<Usuario> listarOperadores(Usuario usuarioPai) {
        CriteriaBuilder builder = getCriteriaBuilder();
        CriteriaQuery<Usuario> query = getCriteriaQuery();
        Root<Usuario> root = query.from(Usuario.class);
        Join<Usuario, Permissao> join = root.join(Usuario_.permissao);
        query.select(root).where(builder.and(builder.equal(root.get(Usuario_.USUARIO_PAI), usuarioPai), builder.equal(join.get(Permissao_.PERMISSAO), "ROLE_OPERATOR")));
        return getEm().createQuery(query).getResultList();
    }
}
