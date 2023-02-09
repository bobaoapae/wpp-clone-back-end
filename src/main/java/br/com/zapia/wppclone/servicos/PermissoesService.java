package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.Permissao;
import br.com.zapia.wppclone.repositorios.CRUDRepository;
import br.com.zapia.wppclone.repositorios.PermissoesRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class PermissoesService extends CRUDService<Permissao> {

    @Autowired
    private PermissoesRepository permissoesRepository;

    public Permissao buscarPermissaoPorNome(String nome) {
        return permissoesRepository.buscarPermissaoPorNome(nome);
    }

    @Override
    public CRUDRepository<Permissao> getRepository() {
        return permissoesRepository;
    }
}
