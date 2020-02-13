package br.com.zapia.wppclone.repositorios;

import br.com.zapia.wppclone.modelo.TrocaDeNumero;
import org.springframework.stereotype.Repository;

@Repository
public class TrocasDeNumerosRepository extends CRUDRepository<TrocaDeNumero> {

    public TrocasDeNumerosRepository() {
        super(TrocaDeNumero.class);
    }
}
