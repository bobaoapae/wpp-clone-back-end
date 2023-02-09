package br.com.zapia.wppclone.modelo;

import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Entity
public class Cobranca extends Entidade {

    @NotNull
    @ManyToOne(optional = false)
    private Usuario usuario;
    @NotNull
    @Column(nullable = false)
    private BigDecimal valor;
    @OneToMany(mappedBy = "cobranca", cascade = CascadeType.ALL)
    private List<Pagamento> pagamentos;
    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Situacao situacao;

    public Cobranca() {
        situacao = Situacao.ATIVA;
        valor = BigDecimal.ZERO;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public List<Pagamento> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<Pagamento> pagamentos) {
        this.pagamentos = pagamentos;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
    }

    public enum Situacao {
        CANCELADA,
        ATIVA,
        CONCLUIDA
    }
}
