package com.desafio.votacao.sessao;

import com.desafio.votacao.sessao.dto.AbrirSessaoRequest;
import com.desafio.votacao.sessao.dto.SessaoResponse;
import com.desafio.votacao.config.OpenApiConfig.Erros;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST da sessao de votacao de uma pauta.
 */
@Tag(name = "Sessoes", description = "Abertura e consulta de sessoes de votacao")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessao")
public class SessaoVotacaoController {

    private final SessaoVotacaoService service;
    private final Clock clock;

    public SessaoVotacaoController(SessaoVotacaoService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    /**
     * Abre a sessao de votacao da pauta.
     *
     * @param pautaId pauta a ser deliberada
     * @param request duracao em minutos; ausente, vale o default configurado (1 minuto)
     * @return a sessao aberta, com a janela e o status
     * @throws com.desafio.votacao.exception.ConflictException se a pauta ja tiver sessao
     */
    @Operation(summary = "Abrir uma sessao de votacao em uma pauta",
            description = "Duracao opcional em minutos no corpo; default 1 minuto. "
                    + "Cada pauta admite uma unica sessao.")
    @ApiResponse(responseCode = "201", description = "Sessao aberta")
    @ApiResponse(responseCode = "400", description = Erros.VALIDACAO)
    @ApiResponse(responseCode = "404", description = Erros.NAO_ENCONTRADO)
    @ApiResponse(responseCode = "409", description = "A pauta ja possui uma sessao de votacao")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse abrir(@PathVariable Long pautaId,
                                @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        return SessaoResponse.from(service.abrir(pautaId, request), LocalDateTime.now(clock));
    }

    /**
     * Consulta a sessao de votacao da pauta.
     *
     * @param pautaId pauta consultada
     * @return a sessao, com status {@code ABERTA} ou {@code ENCERRADA} no instante da consulta
     */
    @Operation(summary = "Consultar a sessao de votacao de uma pauta")
    @ApiResponse(responseCode = "200", description = "Sessao encontrada")
    @ApiResponse(responseCode = "404", description = "A pauta nao possui sessao de votacao")
    @GetMapping
    public SessaoResponse consultar(@PathVariable Long pautaId) {
        return SessaoResponse.from(service.buscarPorPauta(pautaId), LocalDateTime.now(clock));
    }
}
