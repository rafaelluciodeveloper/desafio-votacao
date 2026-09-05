package com.desafio.votacao.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.desafio.votacao.cpf.CpfInvalidoException;
import com.desafio.votacao.cpf.CpfValidationClient;
import com.desafio.votacao.cpf.CpfValidationResult;
import com.desafio.votacao.cpf.StatusVoto;
import com.desafio.votacao.exception.BusinessException;
import com.desafio.votacao.exception.ConflictException;
import com.desafio.votacao.pauta.Pauta;
import com.desafio.votacao.sessao.SessaoVotacao;
import com.desafio.votacao.sessao.SessaoVotacaoService;
import com.desafio.votacao.voto.dto.ResultadoResponse;
import com.desafio.votacao.voto.dto.VotoRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    @Mock
    private VotoRepository votoRepository;
    @Mock
    private SessaoVotacaoService sessaoService;
    @Mock
    private CpfValidationClient cpfValidationClient;

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 5, 10, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.atZone(ZONA).toInstant(), ZONA);

    private VotoService votoService;

    private SessaoVotacao sessaoAberta;
    private final VotoRequest votoSim = new VotoRequest("12345678909", OpcaoVoto.SIM);

    @BeforeEach
    void setup() {
        votoService = new VotoService(votoRepository, sessaoService, cpfValidationClient, RELOGIO);
        Pauta pauta = new Pauta("Pauta teste", "desc", AGORA);
        sessaoAberta = new SessaoVotacao(pauta, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
    }

    @Test
    void deveRegistrarVotoQuandoSessaoAbertaEAssociadoApto() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));
        when(votoRepository.existsBySessaoIdAndAssociadoId(any(), anyString())).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(inv -> inv.getArgument(0));

        Voto voto = votoService.registrar(1L, votoSim);

        assertThat(voto.getOpcao()).isEqualTo(OpcaoVoto.SIM);
        assertThat(voto.getDataVoto()).isEqualTo(AGORA);
        verify(votoRepository).save(any(Voto.class));
    }

    @Test
    void deveFalharQuandoSessaoEncerrada() {
        Pauta pauta = new Pauta("p", "d", AGORA);
        SessaoVotacao encerrada = new SessaoVotacao(pauta, AGORA.minusMinutes(10), AGORA.minusMinutes(5));
        when(sessaoService.buscarPorPauta(1L)).thenReturn(encerrada);

        assertThatThrownBy(() -> votoService.registrar(1L, votoSim))
                .isInstanceOf(BusinessException.class);
        verify(cpfValidationClient, never()).validar(anyString());
        verify(votoRepository, never()).save(any());
    }

    @Test
    void devePropagar404QuandoCpfInvalido() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(cpfValidationClient.validar(anyString())).thenThrow(new CpfInvalidoException("12345678909"));

        assertThatThrownBy(() -> votoService.registrar(1L, votoSim))
                .isInstanceOf(CpfInvalidoException.class);
        verify(votoRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoAssociadoUnableToVote() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.UNABLE_TO_VOTE));

        assertThatThrownBy(() -> votoService.registrar(1L, votoSim))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UNABLE_TO_VOTE");
        verify(votoRepository, never()).save(any());
    }

    @Test
    void deveFalharQuandoAssociadoJaVotou() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));
        when(votoRepository.existsBySessaoIdAndAssociadoId(any(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> votoService.registrar(1L, votoSim))
                .isInstanceOf(ConflictException.class);
        verify(votoRepository, never()).save(any());
    }

    @Test
    void deveTratarCorridaDeVotoDuplicadoComoConflito() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(cpfValidationClient.validar(anyString()))
                .thenReturn(new CpfValidationResult(StatusVoto.ABLE_TO_VOTE));
        when(votoRepository.existsBySessaoIdAndAssociadoId(any(), anyString())).thenReturn(false);
        when(votoRepository.save(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("uk_voto_sessao_associado"));

        assertThatThrownBy(() -> votoService.registrar(1L, votoSim))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deveApurarComoAprovadaQuandoSimMaiorQueNao() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(votoRepository.contarPorOpcao(any())).thenReturn(List.of(
                new ContagemVoto(OpcaoVoto.SIM, 7L), new ContagemVoto(OpcaoVoto.NAO, 3L)));

        ResultadoResponse r = votoService.apurar(1L);

        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.APROVADA);
        assertThat(r.totalVotos()).isEqualTo(10L);
        assertThat(r.votosSim()).isEqualTo(7L);
    }

    @Test
    void deveApurarZerosQuandoNaoHaVotos() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(votoRepository.contarPorOpcao(any())).thenReturn(List.of());

        ResultadoResponse r = votoService.apurar(1L);

        assertThat(r.totalVotos()).isZero();
        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.EMPATE);
    }

    @Test
    void deveApurarComoEmpate() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(votoRepository.contarPorOpcao(any())).thenReturn(List.of(
                new ContagemVoto(OpcaoVoto.SIM, 5L), new ContagemVoto(OpcaoVoto.NAO, 5L)));

        assertThat(votoService.apurar(1L).resultado()).isEqualTo(ResultadoVotacao.EMPATE);
    }
}
