package br.com.zapia.wppclone.modelo;

import br.com.zapia.wppclone.listenners.EntidadeListenner;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@EntityListeners(EntidadeListenner.class)
@MappedSuperclass
public abstract class Entidade implements Serializable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID uuid;
    private boolean ativo;
    @Transient
    private boolean excluido;

    public Entidade() {
        ativo = true;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isExcluido() {
        return excluido;
    }

    public void setExcluido(boolean excluido) {
        this.excluido = excluido;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entidade)) return false;
        Entidade entidade = (Entidade) o;
        return Objects.equals(uuid, entidade.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
