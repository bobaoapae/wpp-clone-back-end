package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.Entidade;
import br.com.zapia.wppclone.repositorios.CRUDRepository;

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

    public boolean desativar(T entidade) {
        return getRepository().desativar(entidade);
    }

    public boolean desativar(UUID uuid) {
        return getRepository().desativar(uuid);
    }

    public boolean ativar(T entidade) {
        return getRepository().ativar(entidade);
    }

    public boolean ativar(UUID uuid) {
        return getRepository().ativar(uuid);
    }

    public T buscar(UUID uuid) {
        return getRepository().buscar(uuid);
    }

    public List<T> listar() {
        return getRepository().listar();
    }

    public abstract CRUDRepository<T> getRepository();
}
