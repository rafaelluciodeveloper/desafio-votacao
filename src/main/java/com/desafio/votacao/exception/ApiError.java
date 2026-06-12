package com.desafio.votacao.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Corpo padronizado de resposta de erro da API.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {
    public record FieldErrorDetail(String field, String message) {
    }
}
