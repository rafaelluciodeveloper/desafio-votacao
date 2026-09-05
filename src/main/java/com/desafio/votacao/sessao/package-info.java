/**
 * Abertura e consulta da sessao de votacao de uma pauta.
 *
 * <p>A sessao e a janela temporal em que os votos sao aceitos: abre no instante da chamada e
 * fecha depois da duracao informada, ou de um minuto por default.
 *
 * <p>Cada pauta admite <strong>uma unica</strong> sessao. Permitir reabertura descartaria a
 * apuracao anterior e, como a unicidade do voto e por sessao, liberaria o mesmo associado para
 * votar de novo na mesma pauta.
 */
package com.desafio.votacao.sessao;
