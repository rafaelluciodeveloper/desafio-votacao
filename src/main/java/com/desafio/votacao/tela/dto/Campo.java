package com.desafio.votacao.tela.dto;

/**
 * Item de uma tela FORMULARIO.
 *
 * @param tipo   TEXTO, NUMERICO, DATA (entrada do usuario) ou LABEL (somente leitura)
 * @param id     chave enviada no corpo da requisicao quando o usuario preenche o campo
 * @param titulo rotulo exibido
 * @param valor  valor pre-preenchido / exibido (usado pelos campos LABEL)
 */
public record Campo(TipoCampo tipo, String id, String titulo, String valor) {

    public enum TipoCampo {
        TEXTO, NUMERICO, DATA, LABEL
    }

    /**
     * Campo de entrada de texto livre.
     *
     * @param id chave enviada no corpo da requisicao
     * @param titulo rotulo exibido
     * @return campo de entrada de texto livre
     */
    public static Campo texto(String id, String titulo) {
        return new Campo(TipoCampo.TEXTO, id, titulo, null);
    }

    /**
     * Campo somente leitura, apenas informativo.
     *
     * @param titulo rotulo exibido
     * @param valor conteudo exibido
     * @return campo apenas informativo, sem entrada do usuario
     */
    public static Campo label(String titulo, Object valor) {
        return new Campo(TipoCampo.LABEL, null, titulo, String.valueOf(valor));
    }
}
