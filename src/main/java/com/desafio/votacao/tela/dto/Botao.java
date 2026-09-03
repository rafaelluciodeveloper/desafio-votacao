package com.desafio.votacao.tela.dto;

import java.util.Map;

/**
 * Botao de acao de uma tela FORMULARIO. O app envia POST para {@code url}
 * com {@code body} acrescido dos valores preenchidos pelo usuario.
 */
public record Botao(String titulo, String url, Map<String, Object> body) {
}
