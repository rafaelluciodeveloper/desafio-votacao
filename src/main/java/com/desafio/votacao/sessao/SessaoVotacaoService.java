package com.desafio.votacao.sessao;

import com.desafio.votacao.config.VotacaoProperties;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.exception.ResourceNotFoundException;
import com.desafio.votacao.pauta.Pauta;
import com.desafio.votacao.pauta.PautaService;
import com.desafio.votacao.sessao.dto.AbrirSessaoRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessaoVotacaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoVotacaoService.class);

    private final SessaoVotacaoRepository repository;
    private final PautaService pautaService;
    private final int duracaoPadraoMinutos;

    public SessaoVotacaoService(SessaoVotacaoRepository repository,
                                PautaService pautaService,
                                VotacaoProperties properties) {
        this.repository = repository;
        this.pautaService = pautaService;
        this.duracaoPadraoMinutos = properties.sessao().duracaoPadraoMinutos();
    }

    @Transactional
    public SessaoVotacao abrir(Long pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaService.buscarPorId(pautaId);

        repository.findFirstByPautaIdOrderByDataAberturaDesc(pautaId).ifPresent(existente -> {
            if (existente.estaAberta(LocalDateTime.now())) {
                throw new ConflictException("Ja existe uma sessao de votacao aberta para a pauta id=" + pautaId);
            }
        });

        int duracao = (request != null && request.duracaoMinutos() != null)
                ? request.duracaoMinutos()
                : duracaoPadraoMinutos;

        LocalDateTime abertura = LocalDateTime.now();
        SessaoVotacao sessao = new SessaoVotacao(pauta, abertura, abertura.plusMinutes(duracao));
        sessao = repository.save(sessao);
        log.info("Sessao aberta id={} pautaId={} duracaoMin={} encerramento={}",
                sessao.getId(), pautaId, duracao, sessao.getDataEncerramento());
        return sessao;
    }

    @Transactional(readOnly = true)
    public SessaoVotacao buscarPorPauta(Long pautaId) {
        return repository.findFirstByPautaIdOrderByDataAberturaDesc(pautaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma sessao de votacao encontrada para a pauta id=" + pautaId));
    }
}
