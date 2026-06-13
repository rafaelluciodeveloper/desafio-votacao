package com.desafio.votacao.sessao;

import com.desafio.votacao.sessao.dto.AbrirSessaoRequest;
import com.desafio.votacao.sessao.dto.SessaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sessoes", description = "Abertura e consulta de sessoes de votacao")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessao")
public class SessaoVotacaoController {

    private final SessaoVotacaoService service;

    public SessaoVotacaoController(SessaoVotacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Abrir uma sessao de votacao em uma pauta",
            description = "Duracao opcional em minutos no corpo; default 1 minuto.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse abrir(@PathVariable Long pautaId,
                                @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        return SessaoResponse.from(service.abrir(pautaId, request));
    }

    @Operation(summary = "Consultar a sessao de votacao de uma pauta")
    @GetMapping
    public SessaoResponse consultar(@PathVariable Long pautaId) {
        return SessaoResponse.from(service.buscarPorPauta(pautaId));
    }
}
