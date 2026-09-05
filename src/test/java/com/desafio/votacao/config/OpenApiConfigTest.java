package com.desafio.votacao.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Protege a documentacao de dois erros que passam despercebidos por serem silenciosos:
 * anunciar um status que a API nao devolve, e anunciar o corpo de erro errado.
 */
class OpenApiConfigTest {

    private static final String ROTA_REST = "/api/v1/pautas/{pautaId}/votos";
    private static final String ROTA_TELA = "/api/v1/ui/votos";

    private final OpenApiConfig config = new OpenApiConfig();
    private OpenAPI openApi;

    @BeforeEach
    void setup() {
        openApi = new OpenAPI().paths(new Paths()
                .addPathItem(ROTA_REST, new PathItem().post(operacao(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("gerado por default"))
                        .addApiResponse("201", new ApiResponse().description("Voto registrado"))
                        .addApiResponse("422", new ApiResponse().description("Sessao encerrada"))
                        .addApiResponse("503", new ApiResponse().description("Servico indisponivel")))))
                .addPathItem(ROTA_TELA, new PathItem().post(operacao(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Tela de confirmacao"))
                        .addApiResponse("409", new ApiResponse().description("Associado ja votou"))))));

        config.corpoPadraoDosErros().customise(openApi);
    }

    @Test
    void deveDescreverInfoDaApi() {
        assertThat(config.votacaoOpenAPI().getInfo().getTitle()).isEqualTo("API de Votacao");
        assertThat(config.votacaoOpenAPI().getInfo().getVersion()).isEqualTo("v1");
    }

    @Test
    void deveRegistrarOSchemaDoCorpoDeErro() {
        assertThat(openApi.getComponents().getSchemas()).containsKey("ApiError");
    }

    @Test
    void deveRemoverO200QuandoAOperacaoDevolve201() {
        assertThat(respostas(ROTA_REST)).doesNotContainKey("200").containsKey("201");
    }

    @Test
    void devePreservarO200DeQuemNaoDevolve201() {
        assertThat(respostas(ROTA_TELA)).containsKey("200");
    }

    @Test
    void rotasRestDevemDocumentarOErroComoApiError() {
        assertThat(refDoCorpo(ROTA_REST, "422")).isEqualTo("#/components/schemas/ApiError");
        assertThat(refDoCorpo(ROTA_REST, "503")).isEqualTo("#/components/schemas/ApiError");
    }

    @Test
    void rotasDeTelaDevemDocumentarOErroComoTela() {
        // A tela e o corpo real dessas rotas; documentar ApiError aqui seria pior que omitir.
        assertThat(refDoCorpo(ROTA_TELA, "409")).isEqualTo("#/components/schemas/TelaFormulario");
    }

    @Test
    void naoDeveMexerNoCorpoDasRespostasDeSucesso() {
        assertThat(respostas(ROTA_TELA).get("200").getContent()).isNull();
    }

    private Operation operacao(ApiResponses respostas) {
        return new Operation().responses(respostas);
    }

    private ApiResponses respostas(String rota) {
        return openApi.getPaths().get(rota).getPost().getResponses();
    }

    private String refDoCorpo(String rota, String codigo) {
        return respostas(rota).get(codigo).getContent().get("application/json").getSchema().get$ref();
    }
}
