package com.desafio.votacao.sessao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

    /**
     * Cada pauta possui no maximo uma sessao de votacao (unicidade garantida no banco).
     */
    Optional<SessaoVotacao> findByPautaId(Long pautaId);
}
