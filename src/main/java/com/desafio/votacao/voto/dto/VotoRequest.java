package com.desafio.votacao.voto.dto;

import com.desafio.votacao.voto.OpcaoVoto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Voto enviado por um associado.
 */
public record VotoRequest(

        @Schema(description = "CPF do associado (somente digitos)", example = "12345678909")
        @NotNull(message = "associadoId e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "associadoId deve conter 11 digitos")
        String associadoId,

        @Schema(description = "Opcao de voto", example = "SIM")
        @NotNull(message = "opcao e obrigatoria (SIM ou NAO)")
        OpcaoVoto opcao
) {
}
