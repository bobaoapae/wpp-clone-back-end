package br.com.zapia.wppclone.modelo.listenners;

import br.com.zapia.wppclone.modelo.Entidade;
import jakarta.persistence.PostRemove;


public class EntidadeListener {

    @PostRemove
    public void postRemove(Entidade entidade) {
        entidade.setExcluido(true);
    }
}
