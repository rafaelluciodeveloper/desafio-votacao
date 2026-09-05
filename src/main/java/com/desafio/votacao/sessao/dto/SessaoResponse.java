package com.desafio.votacao.sessao.dto;

import com.desafio.votacao.sessao.SessaoVotacao;
import java.time.LocalDateTime;

/**
 * Representacao de uma sessao de votacao retornada pela API.
 */
public record SessaoResponse(
        Long id,
        Long pautaId,
        LocalDateTime dataAbertura,
        LocalDateTime dataEncerramento,
        String status
) {
    /**
     * Converte a entidade na representacao exposta pela API.
     *
     * @param sessao entidade de origem
     * @param agora instante usado para decidir se a sessao esta aberta
     * @return a representacao exposta pela API, com o status calculado
     */
    public static SessaoResponse from(SessaoVotacao sessao, LocalDateTime agora) {
        String status = sessao.estaAberta(agora) ? "ABERTA" : "ENCERRADA";
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPauta().getId(),
                sessao.getDataAbertura(),
                sessao.getDataEncerramento(),
                status
        );
    }
}
