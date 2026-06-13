package com.desafio.votacao.voto.dto;

import com.desafio.votacao.voto.OpcaoVoto;
import com.desafio.votacao.voto.Voto;
import java.time.LocalDateTime;

/**
 * Confirmacao de voto registrado.
 */
public record VotoResponse(
        Long id,
        Long sessaoId,
        String associadoId,
        OpcaoVoto opcao,
        LocalDateTime dataVoto
) {
    public static VotoResponse from(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getSessao().getId(),
                voto.getAssociadoId(),
                voto.getOpcao(),
                voto.getDataVoto()
        );
    }
}
