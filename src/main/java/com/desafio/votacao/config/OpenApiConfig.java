package com.desafio.votacao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI votacaoOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API de Votacao")
                .version("v1")
                .description("API REST para gerenciar pautas, sessoes de votacao e votos de associados."));
    }
}
