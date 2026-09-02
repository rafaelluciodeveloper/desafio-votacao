package com.desafio.votacao;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.votacao.cpf.CpfInvalidoException;
import com.desafio.votacao.cpf.CpfValidationClient;
import com.desafio.votacao.cpf.CpfValidationResult;
import com.desafio.votacao.cpf.StatusVoto;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VotacaoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CpfValidationClient cpfValidationClient;

    @Test
    void fluxoCompletoDeVotacao() throws Exception {
        aptoAVotar();
        long pautaId = criarPauta("Pauta integracao");

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ABERTA")));

        votar(pautaId, "11111111111", "SIM");
        votar(pautaId, "22222222222", "SIM");
        votar(pautaId, "33333333333", "NAO");

        mvc.perform(get("/api/v1/pautas/" + pautaId + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votosSim", is(2)))
                .andExpect(jsonPath("$.votosNao", is(1)))
                .andExpect(jsonPath("$.totalVotos", is(3)))
                .andExpect(jsonPath("$.resultado", is("APROVADA")));
    }

    @Test
    void deveUsarUmMinutoQuandoSessaoAbreSemCorpo() throws Exception {
        long pautaId = criarPauta("Sessao default");

        String json = mvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ABERTA")))
                .andReturn().getResponse().getContentAsString();

        String abertura = JsonPath.read(json, "$.dataAbertura");
        String encerramento = JsonPath.read(json, "$.dataEncerramento");
        org.assertj.core.api.Assertions.assertThat(
                        java.time.Duration.between(java.time.LocalDateTime.parse(abertura),
                                java.time.LocalDateTime.parse(encerramento)).toMinutes())
                .isEqualTo(1);
    }

    @Test
    void naoDevePermitirVotoDuplicado() throws Exception {
        aptoAVotar();
        long pautaId = criarPautaComSessao();
        votar(pautaId, "44444444444", "SIM");

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"44444444444\",\"opcao\":\"NAO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar404QuandoCpfInvalido() throws Exception {
        when(cpfValidationClient.validar(anyString()))
                .thenThrow(new CpfInvalidoException("55555555555"));
        long pautaId = criarPautaComSessao();

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"55555555555\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar422QuandoAssociadoNaoApto() throws Exception {
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.UNABLE_TO_VOTE));
        long pautaId = criarPautaComSessao();

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"77777777777\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRejeitarVotoEmSessaoInexistente() throws Exception {
        long pautaId = criarPauta("Sem sessao");

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"66666666666\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveValidarPayloadInvalido() throws Exception {
        mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("titulo")));
    }

    private void aptoAVotar() {
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));
    }

    private long criarPauta(String titulo) throws Exception {
        String json = mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + titulo + "\",\"descricao\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long criarPautaComSessao() throws Exception {
        long pautaId = criarPauta("Pauta");
        mvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isCreated());
        return pautaId;
    }

    private void votar(long pautaId, String cpf, String opcao) throws Exception {
        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"" + cpf + "\",\"opcao\":\"" + opcao + "\"}"))
                .andExpect(status().isCreated());
    }
}
