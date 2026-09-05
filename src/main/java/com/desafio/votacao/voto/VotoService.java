package com.desafio.votacao.voto;

import com.desafio.votacao.cpf.CpfValidationClient;
import com.desafio.votacao.cpf.CpfValidationResult;
import com.desafio.votacao.exception.BusinessException;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.sessao.SessaoVotacao;
import com.desafio.votacao.sessao.SessaoVotacaoService;
import com.desafio.votacao.voto.dto.ResultadoResponse;
import com.desafio.votacao.voto.dto.VotoRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de registro de voto e apuracao.
 */
@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    private final VotoRepository votoRepository;
    private final SessaoVotacaoService sessaoService;
    private final CpfValidationClient cpfValidationClient;
    private final Clock clock;

    public VotoService(VotoRepository votoRepository,
                       SessaoVotacaoService sessaoService,
                       CpfValidationClient cpfValidationClient,
                       Clock clock) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
        this.cpfValidationClient = cpfValidationClient;
        this.clock = clock;
    }

    /**
     * Registra o voto de um associado, na ordem: sessao aberta, associado apto, voto inedito.
     *
     * <p>A unicidade e conferida antes por cortesia, mas quem a garante e a constraint do banco:
     * sob concorrencia, duas requisicoes simultaneas do mesmo associado passariam pela
     * verificacao previa antes de qualquer insercao ser efetivada.
     *
     * @param pautaId pauta em votacao
     * @param request CPF do associado e opcao escolhida
     * @return o voto persistido
     * @throws BusinessException se a sessao estiver fechada ou o associado nao estiver apto
     * @throws ConflictException se o associado ja tiver votado nesta pauta
     * @throws com.desafio.votacao.cpf.CpfInvalidoException se o CPF for invalido
     * @throws com.desafio.votacao.exception.ExternalServiceException se o servico de CPF falhar
     */
    @Transactional
    public Voto registrar(Long pautaId, VotoRequest request) {
        SessaoVotacao sessao = sessaoService.buscarPorPauta(pautaId);

        LocalDateTime agora = LocalDateTime.now(clock);
        if (!sessao.estaAberta(agora)) {
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
            Voto voto = votoRepository.save(
                    new Voto(sessao, request.associadoId(), request.opcao(), agora));
            log.info("Voto registrado id={} sessaoId={} opcao={}", voto.getId(), sessao.getId(), voto.getOpcao());
            return voto;
        } catch (DataIntegrityViolationException ex) {
            // Protege contra corrida: dois votos simultaneos do mesmo associado.
            log.warn("Voto duplicado detectado pela constraint do banco. sessaoId={}", sessao.getId());
            throw new ConflictException("Associado ja votou nesta pauta");
        }
    }

    /**
     * Apura a votacao da pauta.
     *
     * <p>A contagem e feita por agregacao no banco: o custo nao cresce com o numero de votos.
     * A apuracao pode ser consultada com a sessao ainda aberta - o campo
     * {@code sessaoEncerrada} da resposta diz se o resultado ja e definitivo.
     *
     * @param pautaId pauta apurada
     * @return totais por opcao e o resultado consolidado
     * @throws com.desafio.votacao.exception.ResourceNotFoundException se a pauta nao tiver sessao
     */
    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long pautaId) {
        SessaoVotacao sessao = sessaoService.buscarPorPauta(pautaId);

        Map<OpcaoVoto, Long> contagem = votoRepository.contarPorOpcao(sessao.getId()).stream()
                .collect(Collectors.toMap(ContagemVoto::opcao, ContagemVoto::total));
        long votosSim = contagem.getOrDefault(OpcaoVoto.SIM, 0L);
        long votosNao = contagem.getOrDefault(OpcaoVoto.NAO, 0L);

        ResultadoVotacao resultado;
        if (votosSim > votosNao) {
            resultado = ResultadoVotacao.APROVADA;
        } else if (votosNao > votosSim) {
            resultado = ResultadoVotacao.REPROVADA;
        } else {
            resultado = ResultadoVotacao.EMPATE;
        }

        boolean encerrada = !sessao.estaAberta(LocalDateTime.now(clock));
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
