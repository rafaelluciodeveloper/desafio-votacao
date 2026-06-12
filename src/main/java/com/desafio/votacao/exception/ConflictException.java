package com.desafio.votacao.exception;

/**
 * Conflito de estado (ex.: associado ja votou). Mapeada para HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
