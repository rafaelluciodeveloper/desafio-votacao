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
    /**
     * Converte a entidade na confirmacao exposta pela API.
     *
     * @param voto entidade de origem
     * @return o voto no formato de resposta
     */
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
