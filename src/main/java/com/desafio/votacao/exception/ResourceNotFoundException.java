package com.desafio.votacao.exception;

/**
 * Lancada quando um recurso solicitado nao existe. Mapeada para HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
