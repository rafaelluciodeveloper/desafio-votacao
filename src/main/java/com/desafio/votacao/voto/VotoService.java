package com.desafio.votacao.voto;

import com.desafio.votacao.cpf.CpfValidationClient;
import com.desafio.votacao.cpf.CpfValidationResult;
import com.desafio.votacao.exception.BusinessException;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.sessao.SessaoVotacao;
import com.desafio.votacao.sessao.SessaoVotacaoService;
import com.desafio.votacao.voto.dto.ResultadoResponse;
import com.desafio.votacao.voto.dto.VotoRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    private final VotoRepository votoRepository;
    private final SessaoVotacaoService sessaoService;
    private final CpfValidationClient cpfValidationClient;

    public VotoService(VotoRepository votoRepository,
                       SessaoVotacaoService sessaoService,
                       CpfValidationClient cpfValidationClient) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
        this.cpfValidationClient = cpfValidationClient;
    }

    @Transactional
    public Voto registrar(Long pautaId, VotoRequest request) {
        SessaoVotacao sessao = sessaoService.buscarPorPauta(pautaId);

        if (!sessao.estaAberta(LocalDateTime.now())) {
            throw new BusinessException("A sessao de votacao da pauta id=" + pautaId + " nao esta aberta");
        }

        // Tarefa Bonus 1: valida CPF no servico externo. CPF invalido -> 404.
        CpfValidationResult validacao = cpfValidationClient.validar(request.associadoId());
        if (!validacao.apto()) {
            throw new BusinessException("Associado nao esta apto a votar (UNABLE_TO_VOTE)");
        }

        // Verificacao previa amigavel; a unicidade real e garantida pela constraint do banco.
        if (votoRepository.existsBySessaoIdAndAssociadoId(sessao.getId(), request.associadoId())) {
            throw new ConflictException("Associado ja votou nesta pauta");
        }

        try {
            Voto voto = votoRepository.save(new Voto(sessao, request.associadoId(), request.opcao()));
            log.info("Voto registrado id={} sessaoId={} opcao={}", voto.getId(), sessao.getId(), voto.getOpcao());
            return voto;
        } catch (DataIntegrityViolationException ex) {
            // Protege contra corrida: dois votos simultaneos do mesmo associado.
            log.warn("Voto duplicado detectado pela constraint do banco. sessaoId={}", sessao.getId());
            throw new ConflictException("Associado ja votou nesta pauta");
        }
    }

    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long pautaId) {
        SessaoVotacao sessao = sessaoService.buscarPorPauta(pautaId);

        long votosSim = votoRepository.countBySessaoIdAndOpcao(sessao.getId(), OpcaoVoto.SIM);
        long votosNao = votoRepository.countBySessaoIdAndOpcao(sessao.getId(), OpcaoVoto.NAO);

        ResultadoVotacao resultado;
        if (votosSim > votosNao) {
            resultado = ResultadoVotacao.APROVADA;
        } else if (votosNao > votosSim) {
            resultado = ResultadoVotacao.REPROVADA;
        } else {
            resultado = ResultadoVotacao.EMPATE;
        }

        boolean encerrada = !sessao.estaAberta(LocalDateTime.now());
        return new ResultadoResponse(
                pautaId,
                sessao.getId(),
                sessao.getPauta().getTitulo(),
                encerrada,
                votosSim + votosNao,
                votosSim,
                votosNao,
                resultado
        );
    }
}
