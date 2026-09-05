/**
 * Telas JSON consumidas pelo aplicativo mobile, no formato do Anexo 1 do desafio.
 *
 * <p>A navegacao e dirigida pelo servidor: cada tela ja carrega a URL e o corpo que o app deve
 * enviar ao acionar um botao ou item de selecao. O cliente nao conhece rotas nem regra de
 * negocio, e mudancas de fluxo nao exigem republicar o aplicativo.
 *
 * <p>Sao dois tipos de tela: {@code FORMULARIO}, com campos e ate dois botoes de acao, e
 * {@code SELECAO}, com uma lista de opcoes. Erros tambem sao devolvidos como tela - preservando
 * o status HTTP - porque o app so sabe renderizar telas.
 *
 * <p>Esta e uma camada de apresentacao sobre o mesmo dominio: nao duplica regra, delega para os
 * services de pauta, sessao e voto.
 */
package com.desafio.votacao.tela;
