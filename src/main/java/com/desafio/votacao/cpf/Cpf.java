package com.desafio.votacao.cpf;

/**
 * Utilitarios de CPF.
 */
final class Cpf {

    private Cpf() {
    }

    /**
     * Mascara o CPF para log, evitando expor dado pessoal completo.
     */
    static String mascarar(String cpf) {
        if (cpf == null || cpf.length() < 4) {
            return "***";
        }
        return "***" + cpf.substring(cpf.length() - 2);
    }
}
