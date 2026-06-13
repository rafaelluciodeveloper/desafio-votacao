package com.desafio.votacao.cpf;

import com.desafio.votacao.exception.ResourceNotFoundException;

/**
 * CPF considerado invalido pelo servico externo.
 * Conforme especificacao, resulta em HTTP 404 (Not Found).
 */
public class CpfInvalidoException extends ResourceNotFoundException {

    public CpfInvalidoException(String cpf) {
        super("CPF invalido: " + cpf);
    }
}
