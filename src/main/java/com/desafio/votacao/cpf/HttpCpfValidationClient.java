package com.desafio.votacao.cpf;

import com.desafio.votacao.config.VotacaoProperties;
import com.desafio.votacao.exception.ExternalServiceException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client HTTP real do servico externo de validacao de CPF (Tarefa Bonus 1):
 * {@code GET {baseUrl}/users/{cpf}} respondendo {@code {"status":"ABLE_TO_VOTE"}} ou 404.
 * <p>Ativado com {@code votacao.cpf-client.modo=http}. O dominio e configuravel
 * ({@code votacao.cpf-client.base-url}) e os timeouts vem de {@code spring.http.client.*},
 * permitindo apontar para o servico real ou para um mock em emulador/dispositivo.
 */
@Component
@ConditionalOnProperty(prefix = "votacao.cpf-client", name = "modo", havingValue = "http")
public class HttpCpfValidationClient implements CpfValidationClient {

    private static final Logger log = LoggerFactory.getLogger(HttpCpfValidationClient.class);

    private final RestClient restClient;
    private final VotacaoProperties.CpfClient config;

    public HttpCpfValidationClient(RestClient.Builder builder, VotacaoProperties properties) {
        this.config = properties.cpfClient();
        this.restClient = builder.baseUrl(config.baseUrl()).build();
        log.info("Client HTTP de validacao de CPF ativo. baseUrl={}", config.baseUrl());
    }

    @Override
    public CpfValidationResult validar(String cpf) {
        try {
            CpfValidationResult resultado = restClient.get()
                    .uri("/users/{cpf}", cpf)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (req, res) -> {
                        // Um 404 so significa "CPF invalido" se veio do proprio servico (JSON).
                        // Gateways e roteadores tambem respondem 404 (HTML) quando o servico nao
                        // existe mais - tratar isso como CPF invalido reprovaria todo mundo.
                        if (!respondeJson(res)) {
                            throw new ExternalServiceException(
                                    "Servico de validacao de CPF nao encontrado em " + config.baseUrl(), null);
                        }
                        throw new CpfInvalidoException(cpf);
                    })
                    .body(CpfValidationResult.class);

            if (resultado == null || resultado.status() == null) {
                throw new ExternalServiceException(
                        "Resposta invalida do servico de validacao de CPF", null);
            }
            log.info("Validacao de CPF [{}]: valido, status={}", Cpf.mascarar(cpf), resultado.status());
            return resultado;
        } catch (RestClientException ex) {
            throw new ExternalServiceException(
                    "Servico de validacao de CPF indisponivel", ex);
        }
    }

    private boolean respondeJson(ClientHttpResponse response) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        return contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType);
    }
}
