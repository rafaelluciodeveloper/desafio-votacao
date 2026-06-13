package com.desafio.votacao.pauta.dto;

import com.desafio.votacao.pauta.Pauta;
import java.time.LocalDateTime;

/**
 * Representacao de uma pauta retornada pela API.
 */
public record PautaResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime dataCriacao
) {
    public static PautaResponse from(Pauta pauta) {
        return new PautaResponse(
                pauta.getId(),
                pauta.getTitulo(),
                pauta.getDescricao(),
                pauta.getDataCriacao()
        );
    }
}
