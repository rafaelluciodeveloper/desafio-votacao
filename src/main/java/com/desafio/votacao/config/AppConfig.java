package com.desafio.votacao.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VotacaoProperties.class)
public class AppConfig {
}
