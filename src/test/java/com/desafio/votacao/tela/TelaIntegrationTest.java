package com.desafio.votacao.tela;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desafio.votacao.cpf.CpfValidationClient;
import com.desafio.votacao.cpf.CpfValidationResult;
import com.desafio.votacao.cpf.StatusVoto;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato das telas do app mobile (Anexo 1): navegacao dirigida pelo servidor,
 * do menu de pautas ate a apuracao.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TelaIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CpfValidationClient cpfValidationClient;

    @BeforeEach
    void aptoAVotar() {
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));
    }

    @Test
    void telaDePautasDeveSerDoTipoSelecaoComUrlEBodyDeCadaItem() throws Exception {
        long pautaId = criarPautaComSessao("Pauta selecao");

        mvc.perform(get("/api/v1/ui/pautas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo", is("SELECAO")))
                .andExpect(jsonPath("$.itens[?(@.body.pautaId == " + pautaId + ")].url")
                        .value(org.hamcrest.Matchers.hasItem(endsWith("/api/v1/ui/votacao"))));
    }

    @Test
    void telaDeVotacaoDeveSerFormularioComCampoDeCpfEDoisBotoes() throws Exception {
        long pautaId = criarPautaComSessao("Pauta votacao");

        mvc.perform(post("/api/v1/ui/votacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pautaId\":" + pautaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo", is("FORMULARIO")))
                .andExpect(jsonPath("$.itens[?(@.id == 'associadoId')].tipo")
                        .value(org.hamcrest.Matchers.hasItem("TEXTO")))
                .andExpect(jsonPath("$.botoes.length()", is(2)))
                .andExpect(jsonPath("$.botoes[0].body.opcao", is("SIM")))
                .andExpect(jsonPath("$.botoes[1].body.opcao", is("NAO")))
                .andExpect(jsonPath("$.botoes[0].url", containsString("/api/v1/ui/votos")));
    }

    @Test
    void deveVotarEApurarPelasTelas() throws Exception {
        long pautaId = criarPautaComSessao("Pauta fluxo");

        mvc.perform(post("/api/v1/ui/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pautaId\":" + pautaId + ",\"associadoId\":\"10000000001\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo", is("FORMULARIO")))
                .andExpect(jsonPath("$.titulo", is("Voto registrado")));

        mvc.perform(post("/api/v1/ui/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pautaId\":" + pautaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[?(@.titulo == 'Resultado')].valor")
                        .value(org.hamcrest.Matchers.hasItem("APROVADA")));
    }

    @Test
    void erroDeNegocioDeveVirComoTelaFormulario() throws Exception {
        long pautaId = criarPautaComSessao("Pauta duplicada");
        String voto = "{\"pautaId\":" + pautaId + ",\"associadoId\":\"10000000002\",\"opcao\":\"SIM\"}";

        mvc.perform(post("/api/v1/ui/votos").contentType(MediaType.APPLICATION_JSON).content(voto))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/ui/votos").contentType(MediaType.APPLICATION_JSON).content(voto))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.tipo", is("FORMULARIO")))
                .andExpect(jsonPath("$.itens[0].valor", containsString("ja votou")));
    }

    @Test
    void pautaSemSessaoDeveInformarSituacaoNaTela() throws Exception {
        long pautaId = criarPauta("Pauta sem sessao");

        mvc.perform(post("/api/v1/ui/votacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pautaId\":" + pautaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].valor", containsString("Nenhuma sessao")));
    }

    private long criarPauta(String titulo) throws Exception {
        String json = mvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + titulo + "\",\"descricao\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long criarPautaComSessao(String titulo) throws Exception {
        long pautaId = criarPauta(titulo);
        mvc.perform(post("/api/v1/pautas/" + pautaId + "/sessao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isCreated());
        return pautaId;
    }
}
