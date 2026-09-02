package com.desafio.votacao.cpf;

import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementacao Fake do servico de validacao de CPF (Tarefa Bonus 1) - modo default.
 * <p>Decide aleatoriamente se o CPF e valido. Sendo valido, retorna aleatoriamente
 * ABLE_TO_VOTE ou UNABLE_TO_VOTE - portanto o mesmo CPF pode variar entre chamadas.
 * <p>Ative o client HTTP real com {@code votacao.cpf-client.modo=http}.
 */
@Component
@ConditionalOnProperty(prefix = "votacao.cpf-client", name = "modo", havingValue = "fake", matchIfMissing = true)
public class FakeCpfValidationClient implements CpfValidationClient {

    private static final Logger log = LoggerFactory.getLogger(FakeCpfValidationClient.class);

    @Override
    public CpfValidationResult validar(String cpf) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // ~30% dos casos: CPF invalido -> 404
        if (random.nextInt(100) < 30) {
            log.info("Validacao de CPF [{}]: invalido", Cpf.mascarar(cpf));
            throw new CpfInvalidoException(cpf);
        }

        StatusVoto status = random.nextBoolean() ? StatusVoto.ABLE_TO_VOTE : StatusVoto.UNABLE_TO_VOTE;
        log.info("Validacao de CPF [{}]: valido, status={}", Cpf.mascarar(cpf), status);
        return new CpfValidationResult(status);
    }
}
