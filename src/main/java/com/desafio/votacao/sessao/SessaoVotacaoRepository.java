package com.desafio.votacao.sessao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

    /**
     * Recupera a sessao mais recente de uma pauta (uma sessao ativa por vez).
     */
    Optional<SessaoVotacao> findFirstByPautaIdOrderByDataAberturaDesc(Long pautaId);
}
