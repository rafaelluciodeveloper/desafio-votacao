package com.desafio.votacao.voto;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsBySessaoIdAndAssociadoId(Long sessaoId, String associadoId);

    /**
     * Conta votos por opcao usando agregacao no banco (indice idx_voto_sessao_opcao).
     * Evita carregar os votos em memoria - essencial para centenas de milhares de votos.
     */
    long countBySessaoIdAndOpcao(Long sessaoId, OpcaoVoto opcao);
}
