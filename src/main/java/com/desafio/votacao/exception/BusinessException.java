package com.desafio.votacao.exception;

/**
 * Violacao de regra de negocio (ex.: sessao fechada). Mapeada para HTTP 422.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
