package com.desafio.votacao.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VotacaoProperties.class)
public class AppConfig {

    /**
     * Relogio da aplicacao, injetado em vez de chamar {@code LocalDateTime.now()} direto.
     * <p>Deixa o fuso explicito e torna a janela da sessao de votacao verificavel nos testes
     * sem depender do relogio da maquina.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
