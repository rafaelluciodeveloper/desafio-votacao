package com.desafio.votacao.voto;

/**
 * Total de votos de uma opcao, agregado pelo banco.
 */
public record ContagemVoto(OpcaoVoto opcao, long total) {
}
