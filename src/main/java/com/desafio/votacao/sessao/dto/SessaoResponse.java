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
    public static SessaoResponse from(SessaoVotacao sessao) {
        String status = sessao.estaAberta(LocalDateTime.now()) ? "ABERTA" : "ENCERRADA";
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPauta().getId(),
                sessao.getDataAbertura(),
                sessao.getDataEncerramento(),
                status
        );
    }
}
