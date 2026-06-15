package com.desafio.votacao.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private VotoService votoService;

    private SessaoVotacao sessaoAberta;
    private final VotoRequest votoSim = new VotoRequest("12345678909", OpcaoVoto.SIM);

    @BeforeEach
    void setup() {
        Pauta pauta = new Pauta("Pauta teste", "desc");
        LocalDateTime agora = LocalDateTime.now();
        sessaoAberta = new SessaoVotacao(pauta, agora.minusMinutes(1), agora.plusMinutes(5));
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
        verify(votoRepository).save(any(Voto.class));
    }

    @Test
    void deveFalharQuandoSessaoEncerrada() {
        Pauta pauta = new Pauta("p", "d");
        LocalDateTime agora = LocalDateTime.now();
        SessaoVotacao encerrada = new SessaoVotacao(pauta, agora.minusMinutes(10), agora.minusMinutes(5));
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
        when(votoRepository.countBySessaoIdAndOpcao(any(), eq(OpcaoVoto.SIM))).thenReturn(7L);
        when(votoRepository.countBySessaoIdAndOpcao(any(), eq(OpcaoVoto.NAO))).thenReturn(3L);

        ResultadoResponse r = votoService.apurar(1L);

        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.APROVADA);
        assertThat(r.totalVotos()).isEqualTo(10L);
        assertThat(r.votosSim()).isEqualTo(7L);
    }

    @Test
    void deveApurarComoEmpate() {
        when(sessaoService.buscarPorPauta(1L)).thenReturn(sessaoAberta);
        when(votoRepository.countBySessaoIdAndOpcao(any(), eq(OpcaoVoto.SIM))).thenReturn(5L);
        when(votoRepository.countBySessaoIdAndOpcao(any(), eq(OpcaoVoto.NAO))).thenReturn(5L);

        assertThat(votoService.apurar(1L).resultado()).isEqualTo(ResultadoVotacao.EMPATE);
    }
}
