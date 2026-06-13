package com.desafio.votacao.pauta;

import com.desafio.votacao.pauta.dto.PautaRequest;
import com.desafio.votacao.pauta.dto.PautaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Pautas", description = "Cadastro e consulta de pautas")
@RestController
@RequestMapping("/api/v1/pautas")
public class PautaController {

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar uma nova pauta")
    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody PautaRequest request,
                                               UriComponentsBuilder uriBuilder) {
        Pauta pauta = service.criar(request);
        URI location = uriBuilder.path("/api/v1/pautas/{id}").buildAndExpand(pauta.getId()).toUri();
        return ResponseEntity.created(location).body(PautaResponse.from(pauta));
    }

    @Operation(summary = "Listar todas as pautas")
    @GetMapping
    public List<PautaResponse> listar() {
        return service.listar().stream().map(PautaResponse::from).toList();
    }

    @Operation(summary = "Consultar uma pauta por id")
    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return PautaResponse.from(service.buscarPorId(id));
    }
}
