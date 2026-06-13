package com.desafio.votacao.cpf;

/**
 * Resultado da consulta ao servico externo de validacao de CPF.
 */
public record CpfValidationResult(StatusVoto status) {

    public boolean apto() {
        return status == StatusVoto.ABLE_TO_VOTE;
    }
}
