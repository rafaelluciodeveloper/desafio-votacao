package com.desafio.votacao.sessao;

import com.desafio.votacao.pauta.Pauta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sessao de votacao aberta sobre uma pauta, com janela temporal definida.
 */
@Entity
@Table(name = "sessao_votacao")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessaoVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_encerramento", nullable = false)
    private LocalDateTime dataEncerramento;

    public SessaoVotacao(Pauta pauta, LocalDateTime dataAbertura, LocalDateTime dataEncerramento) {
        this.pauta = pauta;
        this.dataAbertura = dataAbertura;
        this.dataEncerramento = dataEncerramento;
    }

    /**
     * Indica se a sessao esta aberta no instante informado.
     */
    public boolean estaAberta(LocalDateTime instante) {
        return !instante.isBefore(dataAbertura) && instante.isBefore(dataEncerramento);
    }
}
