package br.com.zapia.wppclone.servicos;

import br.com.zapia.wppclone.modelo.TrocaDeNumero;
import br.com.zapia.wppclone.repositorios.CRUDRepository;
import br.com.zapia.wppclone.repositorios.TrocasDeNumerosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class TrocasDeNumerosService extends CRUDService<TrocaDeNumero> {

    @Autowired
    private TrocasDeNumerosRepository trocasDeNumerosRepository;

    @Override
    public CRUDRepository<TrocaDeNumero> getRepository() {
        return trocasDeNumerosRepository;
    }
}
