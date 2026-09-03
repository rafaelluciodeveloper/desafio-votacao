package com.desafio.votacao.tela.dto;

import com.desafio.votacao.voto.OpcaoVoto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo enviado pelo app ao acionar um botao de voto: o {@code body} do botao
 * (pautaId e opcao) acrescido do campo preenchido pelo usuario (associadoId).
 */
public record VotoAcaoRequest(
        @NotNull(message = "pautaId e obrigatorio") Long pautaId,
        @NotNull(message = "associadoId e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "associadoId deve conter 11 digitos") String associadoId,
        @NotNull(message = "opcao e obrigatoria (SIM ou NAO)") OpcaoVoto opcao
) {
}
