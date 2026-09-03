package com.desafio.votacao.tela;

import com.desafio.votacao.config.VotacaoProperties;
import com.desafio.votacao.pauta.Pauta;
import com.desafio.votacao.pauta.PautaService;
import com.desafio.votacao.sessao.SessaoVotacao;
import com.desafio.votacao.sessao.SessaoVotacaoService;
import com.desafio.votacao.tela.dto.Botao;
import com.desafio.votacao.tela.dto.Campo;
import com.desafio.votacao.tela.dto.ItemSelecao;
import com.desafio.votacao.tela.dto.PautaAcaoRequest;
import com.desafio.votacao.tela.dto.Tela;
import com.desafio.votacao.tela.dto.TelaFormulario;
import com.desafio.votacao.tela.dto.TelaSelecao;
import com.desafio.votacao.tela.dto.VotoAcaoRequest;
import com.desafio.votacao.voto.OpcaoVoto;
import com.desafio.votacao.voto.VotoService;
import com.desafio.votacao.voto.dto.ResultadoResponse;
import com.desafio.votacao.voto.dto.VotoRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Monta as telas consumidas pelo app mobile (Anexo 1) a partir do dominio de votacao.
 * <p>Nao contem regra de negocio: delega para os services de pauta, sessao e voto e
 * apenas traduz o resultado para o contrato de telas FORMULARIO / SELECAO.
 */
@Service
public class TelaService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    static final String PATH_PAUTAS = "/api/v1/ui/pautas";
    static final String PATH_VOTACAO = "/api/v1/ui/votacao";
    static final String PATH_VOTOS = "/api/v1/ui/votos";
    static final String PATH_RESULTADO = "/api/v1/ui/resultado";

    private final PautaService pautaService;
    private final SessaoVotacaoService sessaoService;
    private final VotoService votoService;
    private final String baseUrlConfigurada;

    public TelaService(PautaService pautaService, SessaoVotacaoService sessaoService,
                       VotoService votoService, VotacaoProperties properties) {
        this.pautaService = pautaService;
        this.sessaoService = sessaoService;
        this.votoService = votoService;
        this.baseUrlConfigurada = properties.ui().baseUrl();
    }

    /**
     * Tela inicial: lista de pautas para o associado escolher.
     */
    public Tela pautas() {
        List<ItemSelecao> itens = pautaService.listar().stream()
                .map(pauta -> new ItemSelecao(
                        pauta.getTitulo(),
                        pauta.getDescricao(),
                        url(PATH_VOTACAO),
                        Map.of("pautaId", pauta.getId())))
                .toList();
        return new TelaSelecao("Pautas em deliberacao", itens);
    }

    /**
     * Tela de votacao da pauta. Se nao houver sessao aberta, devolve a tela de resultado.
     */
    public Tela votacao(PautaAcaoRequest request) {
        Pauta pauta = pautaService.buscarPorId(request.pautaId());
        Optional<SessaoVotacao> sessao = sessaoService.encontrarPorPauta(pauta.getId());

        if (sessao.isEmpty()) {
            return new TelaFormulario(
                    pauta.getTitulo(),
                    List.of(Campo.label("Situacao", "Nenhuma sessao de votacao foi aberta para esta pauta")),
                    List.of(voltar()));
        }
        if (!sessao.get().estaAberta(LocalDateTime.now())) {
            return resultado(new PautaAcaoRequest(pauta.getId()));
        }

        Map<String, Object> body = Map.of("pautaId", pauta.getId());
        return new TelaFormulario(
                pauta.getTitulo(),
                List.of(
                        Campo.label("Descricao", pauta.getDescricao()),
                        Campo.label("Sessao aberta ate", sessao.get().getDataEncerramento().format(HORA)),
                        Campo.texto("associadoId", "Informe seu CPF")),
                List.of(
                        botaoVoto("Sim", body, OpcaoVoto.SIM),
                        botaoVoto("Nao", body, OpcaoVoto.NAO)));
    }

    /**
     * Registra o voto e devolve a tela de confirmacao.
     */
    public Tela votar(VotoAcaoRequest request) {
        votoService.registrar(request.pautaId(), new VotoRequest(request.associadoId(), request.opcao()));
        return new TelaFormulario(
                "Voto registrado",
                List.of(Campo.label("Seu voto", request.opcao().name())),
                List.of(new Botao("Ver resultado", url(PATH_RESULTADO), Map.of("pautaId", request.pautaId())),
                        voltar()));
    }

    /**
     * Tela com a apuracao da pauta.
     */
    public Tela resultado(PautaAcaoRequest request) {
        ResultadoResponse apuracao = votoService.apurar(request.pautaId());
        return new TelaFormulario(
                apuracao.tituloPauta(),
                List.of(
                        Campo.label("Situacao", apuracao.sessaoEncerrada() ? "Sessao encerrada" : "Sessao aberta"),
                        Campo.label("Total de votos", apuracao.totalVotos()),
                        Campo.label("Sim", apuracao.votosSim()),
                        Campo.label("Nao", apuracao.votosNao()),
                        Campo.label("Resultado", apuracao.resultado())),
                List.of(voltar()));
    }

    /**
     * Tela de erro no formato que o app sabe renderizar.
     */
    public Tela erro(String mensagem) {
        return new TelaFormulario(
                "Nao foi possivel concluir",
                List.of(Campo.label("Motivo", mensagem)),
                List.of(voltar()));
    }

    private Botao botaoVoto(String titulo, Map<String, Object> body, OpcaoVoto opcao) {
        return new Botao(titulo, url(PATH_VOTOS),
                Map.of("pautaId", body.get("pautaId"), "opcao", opcao.name()));
    }

    private Botao voltar() {
        return new Botao("Voltar", url(PATH_PAUTAS), Map.of());
    }

    /**
     * Monta a URL de callback. O dominio vem da configuracao ({@code votacao.ui.base-url});
     * vazio, deriva da propria requisicao - o que faz emulador e dispositivo fisico
     * funcionarem sem alteracao de codigo.
     */
    private String url(String path) {
        String base = StringUtils.hasText(baseUrlConfigurada)
                ? baseUrlConfigurada
                : ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        return base + path;
    }
}
