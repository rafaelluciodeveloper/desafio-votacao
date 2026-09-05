package com.desafio.votacao.cpf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.desafio.votacao.config.VotacaoProperties;
import com.desafio.votacao.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpCpfValidationClientTest {

    private static final String BASE_URL = "https://user-info.example";
    private static final String CPF = "12345678909";

    private MockRestServiceServer server;
    private HttpCpfValidationClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        VotacaoProperties properties = new VotacaoProperties(
                new VotacaoProperties.Sessao(1),
                new VotacaoProperties.CpfClient(VotacaoProperties.CpfClient.Modo.HTTP, BASE_URL),
                new VotacaoProperties.Ui(""));
        client = new HttpCpfValidationClient(builder, properties);
    }

    @Test
    void deveRetornarStatusQuandoServicoRespondeComSucesso() {
        server.expect(requestTo(BASE_URL + "/users/" + CPF))
                .andRespond(withSuccess("{\"status\":\"ABLE_TO_VOTE\"}", MediaType.APPLICATION_JSON));

        CpfValidationResult resultado = client.validar(CPF);

        assertThat(resultado.status()).isEqualTo(StatusVoto.ABLE_TO_VOTE);
        assertThat(resultado.apto()).isTrue();
        server.verify();
    }

    @Test
    void deveTraduzir404DoServicoEmCpfInvalido() {
        server.expect(requestTo(BASE_URL + "/users/" + CPF))
                .andRespond(withResourceNotFound().contentType(MediaType.APPLICATION_JSON).body("{}"));

        assertThatThrownBy(() -> client.validar(CPF))
                .isInstanceOf(CpfInvalidoException.class)
                .hasMessageContaining(CPF);
    }

    @Test
    void naoDeveTratar404DeGatewayComoCpfInvalido() {
        // Servico fora do ar: o roteador (ex.: heroku-router "No such app") responde 404 HTML.
        // Tratar isso como CPF invalido reprovaria cada associado silenciosamente.
        server.expect(requestTo(BASE_URL + "/users/" + CPF))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><title>No such app</title></html>"));

        assertThatThrownBy(() -> client.validar(CPF))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining(BASE_URL);
    }

    @Test
    void deveTraduzirFalhaDoServicoEmExternalServiceException() {
        server.expect(requestTo(BASE_URL + "/users/" + CPF)).andRespond(withServerError());

        assertThatThrownBy(() -> client.validar(CPF))
                .isInstanceOf(ExternalServiceException.class);
    }
}
