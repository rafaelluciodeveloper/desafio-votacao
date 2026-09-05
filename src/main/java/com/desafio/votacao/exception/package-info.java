/**
 * Tratamento centralizado de erros.
 *
 * <p>As excecoes de dominio carregam apenas o significado do erro; a traducao para status HTTP
 * fica em um unico lugar ({@link com.desafio.votacao.exception.GlobalExceptionHandler}), de modo
 * que services e controllers nao manipulam codigo HTTP.
 *
 * <p>Todas as respostas de erro compartilham o corpo {@link com.desafio.votacao.exception.ApiError}.
 * A excecao sao as rotas de tela, que devolvem o erro como tela para o app mobile.
 */
package com.desafio.votacao.exception;
