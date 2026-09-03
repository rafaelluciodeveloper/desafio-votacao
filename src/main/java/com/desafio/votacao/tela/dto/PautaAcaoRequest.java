package com.desafio.votacao.tela.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo enviado pelo app ao acionar um item/botao que referencia uma pauta.
 */
public record PautaAcaoRequest(@NotNull(message = "pautaId e obrigatorio") Long pautaId) {
}
