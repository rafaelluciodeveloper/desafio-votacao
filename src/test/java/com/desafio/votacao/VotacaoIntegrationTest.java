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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class VotacaoIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CpfValidationClient cpfValidationClient;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void fluxoCompletoDeVotacao() throws Exception {
        MockMvc mvc = mockMvc();
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));

        String pautaJson = mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pauta integracao\",\"descricao\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long pautaId = objectMapper.readTree(pautaJson).get("id").asLong();

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ABERTA")));

        votar(mvc, pautaId, "11111111111", "SIM");
        votar(mvc, pautaId, "22222222222", "SIM");
        votar(mvc, pautaId, "33333333333", "NAO");

        mvc.perform(get("/api/v1/pautas/" + pautaId + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votosSim", is(2)))
                .andExpect(jsonPath("$.votosNao", is(1)))
                .andExpect(jsonPath("$.totalVotos", is(3)))
                .andExpect(jsonPath("$.resultado", is("APROVADA")));
    }

    @Test
    void naoDevePermitirVotoDuplicado() throws Exception {
        MockMvc mvc = mockMvc();
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));

        long pautaId = criarPautaComSessao(mvc);
        votar(mvc, pautaId, "44444444444", "SIM");

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"44444444444\",\"opcao\":\"NAO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar404QuandoCpfInvalido() throws Exception {
        MockMvc mvc = mockMvc();
        when(cpfValidationClient.validar(anyString()))
                .thenThrow(new CpfInvalidoException("55555555555"));

        long pautaId = criarPautaComSessao(mvc);

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"55555555555\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarVotoEmSessaoInexistente() throws Exception {
        MockMvc mvc = mockMvc();
        String pautaJson = mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sem sessao\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long pautaId = objectMapper.readTree(pautaJson).get("id").asLong();

        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"66666666666\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveValidarPayloadInvalido() throws Exception {
        MockMvc mvc = mockMvc();
        mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"sem titulo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    private long criarPautaComSessao(MockMvc mvc) throws Exception {
        String pautaJson = mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pauta\",\"descricao\":\"d\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long pautaId = objectMapper.readTree(pautaJson).get("id").asLong();
        mvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isCreated());
        return pautaId;
    }

    private void votar(MockMvc mvc, long pautaId, String cpf, String opcao) throws Exception {
        mvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"" + cpf + "\",\"opcao\":\"" + opcao + "\"}"))
                .andExpect(status().isCreated());
    }
}
