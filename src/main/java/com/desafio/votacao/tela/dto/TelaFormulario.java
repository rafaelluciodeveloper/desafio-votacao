package com.desafio.votacao.tela.dto;

import java.util.List;

/**
 * Tela FORMULARIO (Anexo 1): colecao de campos e um ou dois botoes de acao.
 */
public record TelaFormulario(TipoTela tipo, String titulo, List<Campo> itens, List<Botao> botoes) implements Tela {

    public TelaFormulario(String titulo, List<Campo> itens, List<Botao> botoes) {
        this(TipoTela.FORMULARIO, titulo, itens, botoes);
    }
}
