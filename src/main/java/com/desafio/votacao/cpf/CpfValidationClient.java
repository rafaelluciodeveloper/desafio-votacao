package com.desafio.votacao.cpf;

/**
 * Cliente para o servico externo de validacao de CPF.
 * <p>Abstracao que permite trocar a implementacao Fake por uma chamada HTTP real
 * sem impacto nas regras de negocio de votacao.
 */
public interface CpfValidationClient {

    /**
     * Valida o CPF junto ao servico externo.
     *
     * @throws CpfInvalidoException se o CPF for invalido (mapeado para HTTP 404)
     * @return aptidao do associado (ABLE_TO_VOTE / UNABLE_TO_VOTE)
     */
    CpfValidationResult validar(String cpf);
}
