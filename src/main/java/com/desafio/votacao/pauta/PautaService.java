package com.desafio.votacao.pauta;

import com.desafio.votacao.exception.ResourceNotFoundException;
import com.desafio.votacao.pauta.dto.PautaRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PautaService {

    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pauta criar(PautaRequest request) {
        Pauta pauta = repository.save(new Pauta(request.titulo(), request.descricao()));
        log.info("Pauta criada id={} titulo='{}'", pauta.getId(), pauta.getTitulo());
        return pauta;
    }

    @Transactional(readOnly = true)
    public List<Pauta> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pauta buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pauta nao encontrada: id=" + id));
    }
}
