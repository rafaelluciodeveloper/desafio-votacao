package com.desafio.votacao.pauta;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso as pautas persistidas.
 */
public interface PautaRepository extends JpaRepository<Pauta, Long> {
}
