package com.desafio.votacao.cpf;

/**
 * Resultado da consulta ao servico externo de validacao de CPF.
 */
public record CpfValidationResult(StatusVoto status) {

    /**
     * Indica se o associado pode votar.
     *
     * @return {@code true} apenas para {@code ABLE_TO_VOTE}
     */
    public boolean apto() {
        return status == StatusVoto.ABLE_TO_VOTE;
    }
}
