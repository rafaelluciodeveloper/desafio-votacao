package com.desafio.votacao.tela;

import com.desafio.votacao.tela.dto.PautaAcaoRequest;
import com.desafio.votacao.tela.dto.Tela;
import com.desafio.votacao.tela.dto.VotoAcaoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints que devolvem as telas do app mobile no formato do Anexo 1.
 * <p>A navegacao e dirigida pelo servidor: cada tela ja traz a URL e o body que o app
 * deve enviar ao acionar um botao ou item de selecao.
 *
 * <p>Erros tambem sao devolvidos como tela FORMULARIO (o app so sabe renderizar telas),
 * preservando o status HTTP - por isso as respostas de erro aqui nao usam o corpo padrao
 * {@code ApiError} das demais rotas.
 */
@Tag(name = "Telas (app mobile)", description = "Mensagens JSON de tela no formato do Anexo 1")
@RestController
@RequestMapping(TelaRotas.BASE)
public class TelaController {

    private final TelaService service;

    public TelaController(TelaService service) {
        this.service = service;
    }

    @Operation(summary = "Tela SELECAO com as pautas em deliberacao (entrada do fluxo)")
    @RequestMapping(value = TelaRotas.PAUTAS, method = {RequestMethod.GET, RequestMethod.POST})
    public Tela pautas() {
        return service.pautas();
    }

    @Operation(summary = "Tela FORMULARIO de votacao da pauta")
    @PostMapping(TelaRotas.VOTACAO)
    public Tela votacao(@Valid @RequestBody PautaAcaoRequest request) {
        return service.votacao(request);
    }

    @Operation(summary = "Registra o voto e devolve a tela de confirmacao")
    @ApiResponse(responseCode = "200", description = "Tela de confirmacao do voto")
    @ApiResponse(responseCode = "400", description = "Tela de erro: payload invalido")
    @ApiResponse(responseCode = "404", description = "Tela de erro: pauta/sessao inexistente ou CPF invalido")
    @ApiResponse(responseCode = "409", description = "Tela de erro: associado ja votou")
    @ApiResponse(responseCode = "422", description = "Tela de erro: sessao encerrada ou associado inapto")
    @PostMapping(TelaRotas.VOTOS)
    public Tela votar(@Valid @RequestBody VotoAcaoRequest request) {
        return service.votar(request);
    }

    @Operation(summary = "Tela FORMULARIO com a apuracao da pauta")
    @PostMapping(TelaRotas.RESULTADO)
    public Tela resultado(@Valid @RequestBody PautaAcaoRequest request) {
        return service.resultado(request);
    }
}
