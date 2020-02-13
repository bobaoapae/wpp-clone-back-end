package br.com.zapia.wppclone.modelo.listenners;

import br.com.zapia.wppclone.modelo.Entidade;

import javax.persistence.PostRemove;

public class EntidadeListenner {

    @PostRemove
    public void postRemove(Entidade entidade) {
        entidade.setExcluido(true);
    }
}
