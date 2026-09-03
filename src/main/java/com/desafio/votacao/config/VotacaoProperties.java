package com.desafio.votacao.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de dominio configuraveis via application.yml (prefixo "votacao").
 * Validadas na subida da aplicacao para falhar cedo em caso de configuracao invalida.
 */
@Validated
@ConfigurationProperties(prefix = "votacao")
public record VotacaoProperties(@NotNull Sessao sessao, @NotNull CpfClient cpfClient, @NotNull Ui ui) {

    public record Sessao(@Positive int duracaoPadraoMinutos) {
    }

    /**
     * Integracao externa de validacao de CPF. O dominio e o modo sao configuraveis
     * para permitir apontar para o servico real, um mock ou o client fake local.
     * Timeouts ficam em {@code spring.http.client.*}.
     */
    public record CpfClient(@NotNull Modo modo, @NotNull String baseUrl) {

        public enum Modo {
            /** Client fake local, sem dependencia de rede (default). */
            FAKE,
            /** Chamada HTTP real a {baseUrl}/users/{cpf}. */
            HTTP
        }
    }

    /**
     * @param baseUrl dominio usado ao montar as URLs de callback das telas do app mobile.
     *                Vazio = deriva da propria requisicao (util em emulador/dispositivo fisico).
     */
    public record Ui(@NotNull String baseUrl) {
    }
}
