package com.desafio.votacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de dominio configuraveis via application.yml (prefixo "votacao").
 */
@ConfigurationProperties(prefix = "votacao")
public record VotacaoProperties(Sessao sessao, CpfClient cpfClient) {

    public record Sessao(int duracaoPadraoMinutos) {
    }

    public record CpfClient(String baseUrl) {
    }
}
