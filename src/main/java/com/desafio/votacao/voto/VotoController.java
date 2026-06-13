package com.desafio.votacao.voto;

import com.desafio.votacao.voto.dto.ResultadoResponse;
import com.desafio.votacao.voto.dto.VotoRequest;
import com.desafio.votacao.voto.dto.VotoResponse;
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

@Tag(name = "Votos", description = "Registro de votos e apuracao de resultado")
@RestController
@RequestMapping("/api/v1/pautas/{pautaId}")
public class VotoController {

    private final VotoService service;

    public VotoController(VotoService service) {
        this.service = service;
    }

    @Operation(summary = "Registrar voto de um associado na pauta")
    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public VotoResponse votar(@PathVariable Long pautaId, @Valid @RequestBody VotoRequest request) {
        return VotoResponse.from(service.registrar(pautaId, request));
    }

    @Operation(summary = "Contabilizar votos e obter o resultado da pauta")
    @GetMapping("/resultado")
    public ResultadoResponse resultado(@PathVariable Long pautaId) {
        return service.apurar(pautaId);
    }
}
