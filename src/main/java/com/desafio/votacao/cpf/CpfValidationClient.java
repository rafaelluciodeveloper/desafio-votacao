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
     * @param cpf CPF do associado, somente digitos
     * @return aptidao do associado ({@code ABLE_TO_VOTE} / {@code UNABLE_TO_VOTE})
     * @throws CpfInvalidoException se o CPF for invalido (mapeado para HTTP 404)
     * @throws com.desafio.votacao.exception.ExternalServiceException se o servico falhar
     */
    CpfValidationResult validar(String cpf);
}
