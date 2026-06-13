package com.desafio.votacao.voto;

import com.desafio.votacao.sessao.SessaoVotacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Voto de um associado em uma sessao de votacao.
 */
@Entity
@Table(name = "voto", uniqueConstraints =
        @UniqueConstraint(name = "uk_voto_sessao_associado", columnNames = {"sessao_id", "associado_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoVotacao sessao;

    @Column(name = "associado_id", nullable = false, length = 14)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private OpcaoVoto opcao;

    @Column(name = "data_voto", nullable = false)
    private LocalDateTime dataVoto;

    public Voto(SessaoVotacao sessao, String associadoId, OpcaoVoto opcao) {
        this.sessao = sessao;
        this.associadoId = associadoId;
        this.opcao = opcao;
        this.dataVoto = LocalDateTime.now();
    }
}
