package com.desafio.votacao.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.desafio.votacao.config.VotacaoProperties;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.pauta.Pauta;
import com.desafio.votacao.pauta.PautaService;
import com.desafio.votacao.sessao.dto.AbrirSessaoRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoServiceTest {

    @Mock
    private SessaoVotacaoRepository repository;
    @Mock
    private PautaService pautaService;

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 5, 10, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);

    private SessaoVotacaoService service;

    @BeforeEach
    void setup() {
        VotacaoProperties props = new VotacaoProperties(
                new VotacaoProperties.Sessao(1),
                new VotacaoProperties.CpfClient(VotacaoProperties.CpfClient.Modo.FAKE, "http://localhost"),
                new VotacaoProperties.Ui(""));
        service = new SessaoVotacaoService(repository, pautaService, props, RELOGIO);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta());
    }

    @Test
    void deveUsarDuracaoPadraoDeUmMinutoQuandoNaoInformada() {
        when(repository.findByPautaId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(SessaoVotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(1L, null);

        assertThat(sessao.getDataAbertura()).isEqualTo(AGORA);
        assertThat(sessao.getDataEncerramento()).isEqualTo(AGORA.plusMinutes(1));
    }

    @Test
    void deveUsarDuracaoInformada() {
        when(repository.findByPautaId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(SessaoVotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(1L, new AbrirSessaoRequest(15));

        assertThat(sessao.getDataEncerramento()).isEqualTo(AGORA.plusMinutes(15));
    }

    @Test
    void deveImpedirAberturaSeJaExisteSessaoAberta() {
        SessaoVotacao aberta = new SessaoVotacao(pauta(), AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        when(repository.findByPautaId(1L)).thenReturn(Optional.of(aberta));

        assertThatThrownBy(() -> service.abrir(1L, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveImpedirReaberturaDeSessaoJaEncerrada() {
        SessaoVotacao encerrada = new SessaoVotacao(pauta(), AGORA.minusMinutes(10), AGORA.minusMinutes(5));
        when(repository.findByPautaId(1L)).thenReturn(Optional.of(encerrada));

        // Reabrir descartaria a apuracao e permitiria voto em dobro na mesma pauta.
        assertThatThrownBy(() -> service.abrir(1L, null))
                .isInstanceOf(ConflictException.class);
    }

    private Pauta pauta() {
        return new Pauta("p", "d", AGORA);
    }
}
