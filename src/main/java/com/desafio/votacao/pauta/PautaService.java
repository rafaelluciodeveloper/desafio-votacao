package com.desafio.votacao.pauta;

import com.desafio.votacao.exception.ResourceNotFoundException;
import com.desafio.votacao.pauta.dto.PautaRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de cadastro e consulta de pautas.
 */
@Service
public class PautaService {

    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository repository;
    private final Clock clock;

    public PautaService(PautaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Cadastra uma nova pauta, datada pelo relogio da aplicacao.
     *
     * @param request dados informados na chamada
     * @return a pauta persistida
     */
    @Transactional
    public Pauta criar(PautaRequest request) {
        Pauta pauta = repository.save(
                new Pauta(request.titulo(), request.descricao(), LocalDateTime.now(clock)));
        log.info("Pauta criada id={} titulo='{}'", pauta.getId(), pauta.getTitulo());
        return pauta;
    }

    /**
     * Lista as pautas cadastradas.
     *
     * @return todas as pautas
     */
    @Transactional(readOnly = true)
    public List<Pauta> listar() {
        return repository.findAll();
    }

    /**
     * Busca uma pauta pelo identificador.
     *
     * @param id identificador da pauta
     * @return a pauta correspondente
     * @throws ResourceNotFoundException se nao existir pauta com esse id
     */
    @Transactional(readOnly = true)
    public Pauta buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pauta nao encontrada: id=" + id));
    }
}
