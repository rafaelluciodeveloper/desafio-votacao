package com.desafio.votacao.tela;

import com.desafio.votacao.exception.BusinessException;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.exception.ExternalServiceException;
import com.desafio.votacao.exception.ResourceNotFoundException;
import com.desafio.votacao.tela.dto.Tela;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erros nas rotas de tela sao devolvidos como uma tela FORMULARIO - o app mobile
 * so sabe renderizar telas. O status HTTP correto e preservado.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = TelaController.class)
public class TelaExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TelaExceptionHandler.class);

    private final TelaService service;

    public TelaExceptionHandler(TelaService service) {
        this.service = service;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Tela> notFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Tela> conflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Tela> business(BusinessException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Tela> external(ExternalServiceException ex) {
        log.error("Falha na integracao externa durante montagem de tela", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Tela> validation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Dados invalidos");
        return build(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseEntity<Tela> build(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(service.erro(mensagem));
    }
}
