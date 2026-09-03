package com.desafio.votacao.tela.dto;

import java.util.List;

/**
 * Tela SELECAO (Anexo 1): lista de opcoes acionaveis pelo usuario.
 */
public record TelaSelecao(TipoTela tipo, String titulo, List<ItemSelecao> itens) implements Tela {

    public TelaSelecao(String titulo, List<ItemSelecao> itens) {
        this(TipoTela.SELECAO, titulo, itens);
    }
}
