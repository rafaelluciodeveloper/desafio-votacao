package com.desafio.votacao.voto.dto;

import com.desafio.votacao.voto.ResultadoVotacao;

/**
 * Resultado consolidado da votacao de uma pauta.
 */
public record ResultadoResponse(
        Long pautaId,
        Long sessaoId,
        String tituloPauta,
        boolean sessaoEncerrada,
        long totalVotos,
        long votosSim,
        long votosNao,
        ResultadoVotacao resultado
) {
}
