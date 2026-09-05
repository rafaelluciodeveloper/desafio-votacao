package com.desafio.votacao.cpf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * O client fake e aleatorio por contrato ("um mesmo CPF pode funcionar em um teste e nao
 * funcionar no outro"), entao o que se verifica sao as invariantes ao longo de muitas
 * chamadas, nao o retorno de uma chamada isolada.
 */
class FakeCpfValidationClientTest {

    private static final int CHAMADAS = 500;
    private static final String CPF = "12345678909";

    private final FakeCpfValidationClient client = new FakeCpfValidationClient();

    @Test
    void deveProduzirOsTresDesfechosAoLongoDeVariasChamadas() {
        Set<StatusVoto> statusVistos = EnumSet.noneOf(StatusVoto.class);
        int invalidos = 0;

        for (int i = 0; i < CHAMADAS; i++) {
            try {
                CpfValidationResult resultado = client.validar(CPF);
                assertThat(resultado.status()).isNotNull();
                statusVistos.add(resultado.status());
            } catch (CpfInvalidoException ex) {
                invalidos++;
                assertThat(ex.getMessage()).contains(CPF);
            }
        }

        assertThat(invalidos).as("CPFs recusados como invalidos").isPositive();
        assertThat(statusVistos).containsExactlyInAnyOrder(StatusVoto.ABLE_TO_VOTE, StatusVoto.UNABLE_TO_VOTE);
    }

    @Test
    void aptoDeveRefletirApenasAbleToVote() {
        assertThat(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE).apto()).isTrue();
        assertThat(new CpfValidationResult(StatusVoto.UNABLE_TO_VOTE).apto()).isFalse();
    }

    @Test
    void deveMascararCpfParaNaoExporDadoPessoalNoLog() {
        assertThat(Cpf.mascarar("12345678909")).isEqualTo("***09");
        assertThat(Cpf.mascarar("123")).isEqualTo("***");
        assertThat(Cpf.mascarar(null)).isEqualTo("***");
    }
}
