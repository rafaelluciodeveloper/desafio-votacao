/**
 * API REST para gerenciar pautas, sessoes de votacao e votos de associados em assembleias
 * de cooperativas.
 *
 * <p>O codigo e organizado <strong>por feature</strong>, nao por camada tecnica: cada pacote
 * de dominio concentra entidade, repositorio, service, controller e DTOs, de modo que uma
 * mudanca de comportamento fique contida em uma pasta.
 *
 * <ul>
 *   <li>{@link com.desafio.votacao.pauta} - cadastro e consulta de pautas</li>
 *   <li>{@link com.desafio.votacao.sessao} - abertura e consulta da sessao de votacao</li>
 *   <li>{@link com.desafio.votacao.voto} - registro de votos e apuracao</li>
 *   <li>{@link com.desafio.votacao.tela} - telas JSON consumidas pelo app mobile (Anexo 1)</li>
 *   <li>{@link com.desafio.votacao.cpf} - integracao externa de validacao de CPF</li>
 *   <li>{@link com.desafio.votacao.exception} - tratamento centralizado de erros</li>
 *   <li>{@link com.desafio.votacao.config} - propriedades, relogio e OpenAPI</li>
 * </ul>
 *
 * <p>Duas regras do dominio sao garantidas <em>no banco</em>, e nao apenas em memoria, porque
 * checagem em memoria nao sobrevive a concorrencia: cada associado vota uma unica vez por
 * sessao ({@code uk_voto_sessao_associado}) e cada pauta tem uma unica sessao
 * ({@code uk_sessao_pauta}).
 */
package com.desafio.votacao;
