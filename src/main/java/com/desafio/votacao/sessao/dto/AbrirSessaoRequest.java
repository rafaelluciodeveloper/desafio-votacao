package com.desafio.votacao.sessao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/**
 * Dados para abertura de uma sessao de votacao.
 * Se {@code duracaoMinutos} for omitido, usa-se o default configurado (1 minuto).
 */
public record AbrirSessaoRequest(

        @Schema(description = "Duracao da sessao em minutos. Default: 1 minuto.", example = "5")
        @Positive(message = "duracaoMinutos deve ser positivo")
        Integer duracaoMinutos
) {
}
