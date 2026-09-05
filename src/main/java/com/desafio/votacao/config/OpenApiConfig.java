package com.desafio.votacao.config;

import com.desafio.votacao.exception.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacao OpenAPI da API.
 */
@Configuration
public class OpenApiConfig {

    private static final String REF_API_ERROR = "#/components/schemas/ApiError";
    private static final String REF_TELA = "#/components/schemas/TelaFormulario";
    private static final String PREFIXO_TELAS = "/api/v1/ui";

    /**
     * Metadados da especificacao publicada no Swagger UI.
     *
     * @return titulo, versao e descricao da API
     */
    @Bean
    public OpenAPI votacaoOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API de Votacao")
                .version("v1")
                .description("API REST para gerenciar pautas, sessoes de votacao e votos de associados."));
    }

    /**
     * As operacoes declaram apenas o codigo e a descricao de cada erro; o corpo e sempre o mesmo
     * {@link ApiError}, preenchido aqui em vez de repetido em cada anotacao.
     *
     * <p>As rotas de tela sao a excecao: elas devolvem erro como tela FORMULARIO, nao como
     * {@code ApiError} - documentar o corpo errado ali seria pior que nao documentar.
     *
     * <p>Tambem remove o 200 que o springdoc assume por padrao quando a operacao ja declara 201:
     * sem isso a documentacao anuncia um status que a API nunca devolve.
     *
     * @return o customizador aplicado a especificacao gerada
     */
    @Bean
    public OpenApiCustomizer corpoPadraoDosErros() {
        return openApi -> {
            registrarSchemaApiError(openApi);
            Content corpoApiError = corpoJson(REF_API_ERROR);
            Content corpoTela = corpoJson(REF_TELA);

            openApi.getPaths().forEach((caminho, pathItem) -> {
                Content corpoDeErro = caminho.startsWith(PREFIXO_TELAS) ? corpoTela : corpoApiError;
                pathItem.readOperations().forEach(operacao -> {
                    ApiResponses respostas = operacao.getResponses();
                    if (respostas.containsKey("201")) {
                        respostas.remove("200");
                    }
                    respostas.forEach((codigo, resposta) -> {
                        if (codigo.startsWith("4") || codigo.startsWith("5")) {
                            resposta.setContent(corpoDeErro);
                        }
                    });
                });
            });
        };
    }

    private Content corpoJson(String ref) {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref(ref)));
    }

    private void registrarSchemaApiError(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        ModelConverters.getInstance()
                .readAll(ApiError.class)
                .forEach(openApi.getComponents()::addSchemas);
    }

    /**
     * Erros comuns a toda a API, para reuso nas anotacoes dos controllers.
     */
    public static final class Erros {
        public static final String VALIDACAO = "Payload invalido (campos obrigatorios ou formato)";
        public static final String NAO_ENCONTRADO = "Recurso nao encontrado";
        public static final String CONFLITO = "Conflito de estado";
        public static final String REGRA = "Regra de negocio violada";
        public static final String EXTERNO = "Servico externo de validacao de CPF indisponivel";

        private Erros() {
        }
    }
}
