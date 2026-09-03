package com.desafio.votacao.tela.dto;

/**
 * Contrato comum das telas suportadas pelo app mobile (Anexo 1).
 */
public sealed interface Tela permits TelaFormulario, TelaSelecao {

    TipoTela tipo();

    String titulo();

    enum TipoTela {
        FORMULARIO, SELECAO
    }
}
