package com.desafio.votacao.voto;

import com.desafio.votacao.voto.dto.ResultadoResponse;
import com.desafio.votacao.voto.dto.VotoRequest;
import com.desafio.votacao.voto.dto.VotoResponse;
import com.desafio.votacao.config.OpenApiConfig.Erros;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "Votos", description = "Registro de votos e apuracao de resultado")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}")
public class VotoController {

    private final VotoService service;

    public VotoController(VotoService service) {
        this.service = service;
    }

    @Operation(summary = "Registrar voto de um associado na pauta")
    @ApiResponse(responseCode = "201", description = "Voto registrado")
    @ApiResponse(responseCode = "400", description = Erros.VALIDACAO)
    @ApiResponse(responseCode = "404", description = "Sessao inexistente ou CPF invalido")
    @ApiResponse(responseCode = "409", description = "Associado ja votou nesta pauta")
    @ApiResponse(responseCode = "422", description = "Sessao encerrada ou associado UNABLE_TO_VOTE")
    @ApiResponse(responseCode = "503", description = Erros.EXTERNO)
    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public VotoResponse votar(@PathVariable Long pautaId, @Valid @RequestBody VotoRequest request) {
        return VotoResponse.from(service.registrar(pautaId, request));
    }

    @Operation(summary = "Contabilizar votos e obter o resultado da pauta")
    @ApiResponse(responseCode = "200", description = "Apuracao da pauta")
    @ApiResponse(responseCode = "404", description = "A pauta nao possui sessao de votacao")
    @GetMapping("/resultado")
    public ResultadoResponse resultado(@PathVariable Long pautaId) {
        return service.apurar(pautaId);
    }
}
