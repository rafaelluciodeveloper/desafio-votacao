package com.desafio.votacao.sessao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso as sessoes de votacao persistidas.
 */
public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

    /**
     * Cada pauta possui no maximo uma sessao de votacao (unicidade garantida no banco).
     *
     * @param pautaId pauta consultada
     * @return a sessao da pauta, ou vazio se ainda nao houver
     */
    Optional<SessaoVotacao> findByPautaId(Long pautaId);
}
