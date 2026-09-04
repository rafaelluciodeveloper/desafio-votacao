package com.desafio.votacao.voto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsBySessaoIdAndAssociadoId(Long sessaoId, String associadoId);

    /**
     * Apuracao por agregacao no banco, em uma unica varredura (indice idx_voto_sessao_opcao).
     * Nao carrega votos em memoria: o custo de transferencia e constante, independente do
     * volume - essencial para centenas de milhares de votos.
     */
    @Query("""
            select new com.desafio.votacao.voto.ContagemVoto(v.opcao, count(v))
            from Voto v
            where v.sessao.id = :sessaoId
            group by v.opcao
            """)
    List<ContagemVoto> contarPorOpcao(Long sessaoId);
}
