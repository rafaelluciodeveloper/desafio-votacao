package com.desafio.votacao.sessao;

import com.desafio.votacao.config.VotacaoProperties;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.exception.ResourceNotFoundException;
import com.desafio.votacao.pauta.Pauta;
import com.desafio.votacao.pauta.PautaService;
import com.desafio.votacao.sessao.dto.AbrirSessaoRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de abertura e consulta da sessao de votacao.
 */
@Service
public class SessaoVotacaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoVotacaoService.class);

    private final SessaoVotacaoRepository repository;
    private final PautaService pautaService;
    private final Clock clock;
    private final int duracaoPadraoMinutos;

    public SessaoVotacaoService(SessaoVotacaoRepository repository,
                                PautaService pautaService,
                                VotacaoProperties properties,
                                Clock clock) {
        this.repository = repository;
        this.pautaService = pautaService;
        this.clock = clock;
        this.duracaoPadraoMinutos = properties.sessao().duracaoPadraoMinutos();
    }

    /**
     * Abre a sessao de votacao da pauta, a partir do instante atual.
     *
     * @param pautaId pauta a ser deliberada
     * @param request duracao em minutos; {@code null} ou sem duracao usa o default configurado
     * @return a sessao persistida
     * @throws ConflictException se a pauta ja possuir sessao, aberta ou encerrada
     * @throws com.desafio.votacao.exception.ResourceNotFoundException se a pauta nao existir
     */
    @Transactional
    public SessaoVotacao abrir(Long pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaService.buscarPorId(pautaId);

        // Uma pauta e deliberada uma unica vez: reabrir a sessao permitiria que o mesmo
        // associado votasse novamente e descartaria a apuracao anterior.
        repository.findByPautaId(pautaId).ifPresent(existente -> {
            throw new ConflictException("A pauta id=" + pautaId + " ja possui uma sessao de votacao");
        });

        int duracao = (request != null && request.duracaoMinutos() != null)
                ? request.duracaoMinutos()
                : duracaoPadraoMinutos;

        LocalDateTime abertura = LocalDateTime.now(clock);
        SessaoVotacao sessao = new SessaoVotacao(pauta, abertura, abertura.plusMinutes(duracao));
        sessao = repository.save(sessao);
        log.info("Sessao aberta id={} pautaId={} duracaoMin={} encerramento={}",
                sessao.getId(), pautaId, duracao, sessao.getDataEncerramento());
        return sessao;
    }

    /**
     * Busca a sessao de votacao da pauta.
     *
     * @param pautaId pauta consultada
     * @return a sessao da pauta
     * @throws ResourceNotFoundException se a pauta ainda nao tiver sessao
     */
    @Transactional(readOnly = true)
    public SessaoVotacao buscarPorPauta(Long pautaId) {
        // Chama o repositorio direto: auto-invocacao nao passa pelo proxy transacional.
        return repository.findByPautaId(pautaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma sessao de votacao encontrada para a pauta id=" + pautaId));
    }

    /**
     * Variante sem excecao, para fluxos que precisam decidir com base na ausencia de sessao
     * (ex.: montagem das telas do app mobile).
     *
     * @param pautaId pauta consultada
     * @return a sessao da pauta, ou vazio se ainda nao houver
     */
    @Transactional(readOnly = true)
    public Optional<SessaoVotacao> encontrarPorPauta(Long pautaId) {
        return repository.findByPautaId(pautaId);
    }
}
