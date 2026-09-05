package com.desafio.votacao.pauta;

import com.desafio.votacao.pauta.dto.PautaRequest;
import com.desafio.votacao.pauta.dto.PautaResponse;
import com.desafio.votacao.config.OpenApiConfig.Erros;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

/**
 * Endpoints REST de pauta.
 */
@Tag(name = "Pautas", description = "Cadastro e consulta de pautas")
@RestController
@RequestMapping("/api/v1/pautas")
public class PautaController {

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    /**
     * Cadastra uma pauta e devolve {@code 201} com o cabecalho {@code Location} do recurso criado.
     *
     * @param request titulo e descricao da pauta
     * @param uriBuilder base para montar o {@code Location} da pauta criada
     * @return a pauta cadastrada, ja com id
     */
    @Operation(summary = "Cadastrar uma nova pauta")
    @ApiResponse(responseCode = "201", description = "Pauta cadastrada")
    @ApiResponse(responseCode = "400", description = Erros.VALIDACAO)
    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody PautaRequest request,
                                               UriComponentsBuilder uriBuilder) {
        Pauta pauta = service.criar(request);
        URI location = uriBuilder.path("/api/v1/pautas/{id}").buildAndExpand(pauta.getId()).toUri();
        return ResponseEntity.created(location).body(PautaResponse.from(pauta));
    }

    /**
     * Lista as pautas cadastradas.
     *
     * @return todas as pautas
     */
    @Operation(summary = "Listar todas as pautas")
    @GetMapping
    public List<PautaResponse> listar() {
        return service.listar().stream().map(PautaResponse::from).toList();
    }

    /**
     * Consulta uma pauta pelo identificador.
     *
     * @param id identificador da pauta
     * @return a pauta correspondente
     * @throws com.desafio.votacao.exception.ResourceNotFoundException se a pauta nao existir
     */
    @Operation(summary = "Consultar uma pauta por id")
    @ApiResponse(responseCode = "200", description = "Pauta encontrada")
    @ApiResponse(responseCode = "404", description = Erros.NAO_ENCONTRADO)
    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return PautaResponse.from(service.buscarPorId(id));
    }
}
