package com.desafio.votacao.tela;

/**
 * Rotas das telas do app mobile, em um unico lugar.
 *
 * <p>O servidor precisa das duas pontas: o {@link TelaController} mapeia a rota e o
 * {@link TelaService} monta a URL de callback que o app vai chamar. Escritas separadamente,
 * uma mudanca em uma das pontas faria as telas apontarem para uma rota inexistente sem
 * nenhum erro de compilacao.
 */
public final class TelaRotas {

    /** Prefixo das telas, versionado junto com o resto da API. */
    public static final String BASE = "/api/v1/ui";

    public static final String PAUTAS = "/pautas";
    public static final String VOTACAO = "/votacao";
    public static final String VOTOS = "/votos";
    public static final String RESULTADO = "/resultado";

    static final String URL_PAUTAS = BASE + PAUTAS;
    static final String URL_VOTACAO = BASE + VOTACAO;
    static final String URL_VOTOS = BASE + VOTOS;
    static final String URL_RESULTADO = BASE + RESULTADO;

    private TelaRotas() {
    }
}
