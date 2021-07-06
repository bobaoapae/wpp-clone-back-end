package br.com.zapia.wppclone.modelo;

import br.com.zapia.wppclone.modelo.listenners.EntidadeListener;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@EntityListeners(EntidadeListener.class)
@MappedSuperclass
public abstract class Entidade implements Serializable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @NotNull
    @Column(updatable = false, nullable = false)
    private UUID uuid;
    @NotNull
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean ativo;
    @CreationTimestamp
    private LocalDateTime criadoEm;
    @UpdateTimestamp
    private LocalDateTime atualizadoEm;
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

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
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
