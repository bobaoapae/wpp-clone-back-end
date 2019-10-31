package br.com.zapia.catarin.servicos;

import br.com.zapia.catarin.modelo.Entidade;
import br.com.zapia.catarin.repositorios.CRUDRepository;

import java.util.List;
import java.util.UUID;

public abstract class CRUDService<T extends Entidade> {

    public boolean salvar(T entidade) {
        return getRepository().salvar(entidade);
    }

    public boolean remover(T entidade) {
        return getRepository().remover(entidade);
    }

    public boolean remover(UUID uuid) {
        return getRepository().remover(uuid);
    }

    public T buscar(UUID uuid) {
        return getRepository().buscar(uuid);
    }

    public List<T> listar() {
        return getRepository().listar();
    }

    public abstract CRUDRepository<T> getRepository();
}
