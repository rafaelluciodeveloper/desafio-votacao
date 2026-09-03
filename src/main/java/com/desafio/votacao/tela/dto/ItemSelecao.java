package com.desafio.votacao.tela.dto;

import java.util.Map;

/**
 * Opcao de uma tela SELECAO. O app envia POST para {@code url} com {@code body}
 * quando o item e acionado.
 */
public record ItemSelecao(String titulo, String descricao, String url, Map<String, Object> body) {
}
