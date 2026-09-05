/**
 * Registro de votos e apuracao do resultado.
 *
 * <p>O voto e {@code SIM} ou {@code NAO}, e cada associado vota uma unica vez por pauta -
 * garantido por constraint no banco, nao apenas pela verificacao previa em memoria.
 *
 * <p>A apuracao usa agregacao no banco ({@code COUNT ... GROUP BY opcao}) em vez de carregar os
 * votos: o custo da resposta nao cresce com o volume, o que sustenta o cenario de centenas de
 * milhares de votos.
 */
package com.desafio.votacao.voto;
