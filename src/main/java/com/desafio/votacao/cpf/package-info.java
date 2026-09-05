/**
 * Integracao com o servico externo que informa se um associado pode votar (Tarefa Bonus 1).
 *
 * <p>{@link com.desafio.votacao.cpf.CpfValidationClient} e a abstracao; a implementacao ativa e
 * escolhida por configuracao ({@code votacao.cpf-client.modo}), sem condicional no dominio:
 *
 * <ul>
 *   <li>{@code fake} - {@link com.desafio.votacao.cpf.FakeCpfValidationClient}, aleatorio e
 *       local, default para a aplicacao rodar sem dependencia de rede</li>
 *   <li>{@code http} - {@link com.desafio.votacao.cpf.HttpCpfValidationClient}, chamada real</li>
 * </ul>
 */
package com.desafio.votacao.cpf;
