package com.desafio.votacao.pauta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastro de uma nova pauta.
 */
public record PautaRequest(

        @Schema(example = "Reforma do estatuto")
        @NotBlank(message = "titulo e obrigatorio")
        @Size(max = 150, message = "titulo deve ter no maximo 150 caracteres")
        String titulo,

        @Schema(example = "Votacao para aprovar a reforma do estatuto da cooperativa")
        @Size(max = 2000, message = "descricao deve ter no maximo 2000 caracteres")
        String descricao
) {
}
