package com.desafio.votacao.exception;

/**
 * Falha ao consultar um sistema externo (indisponivel, timeout, resposta inesperada).
 * Mapeada para HTTP 503 - o erro nao e do cliente, e a dependencia que falhou.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
