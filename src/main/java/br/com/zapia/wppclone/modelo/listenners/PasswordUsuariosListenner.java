package br.com.zapia.wppclone.modelo.listenners;

import br.com.zapia.wppclone.modelo.Usuario;
import br.com.zapia.wppclone.utils.Util;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

public class PasswordUsuariosListenner {

    @PrePersist
    public void prePersist(Usuario usuario) {
        usuario.setSenha(Util.encodePassword(usuario.getSenha()));
    }

    @PreUpdate
    public void preUpdate(Usuario usuario) {
        if (usuario.isUpdateSenha()) {
            usuario.setSenha(Util.encodePassword(usuario.getSenha()));
        }
    }
}
