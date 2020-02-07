package br.com.zapia.wppclone.modelo;

import br.com.zapia.wppclone.modelo.enums.FormaPagamento;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
public class Pagamento extends Entidade {

    @NotNull
    @ManyToOne(optional = false)
    private Cobranca cobranca;
    @NotNull
    @Column(nullable = false)
    private BigDecimal valor;
    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Situacao situacao;
    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;
    @NotBlank
    @Column(nullable = false)
    private String referencia;

    public Pagamento() {
        situacao = Situacao.PROCESSANDO;
        formaPagamento = FormaPagamento.MANUAL;
        valor = BigDecimal.ZERO;
    }

    public Cobranca getCobranca() {
        return cobranca;
    }

    public void setCobranca(Cobranca cobranca) {
        this.cobranca = cobranca;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public enum Situacao {
        PROCESSANDO,
        RECUSADO,
        ACEITO,
        ESTORNADO
    }

}
