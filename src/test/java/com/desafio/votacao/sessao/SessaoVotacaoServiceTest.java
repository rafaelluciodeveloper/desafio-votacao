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
import java.time.Duration;
import java.time.LocalDateTime;
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

    private SessaoVotacaoService service;

    @BeforeEach
    void setup() {
        VotacaoProperties props = new VotacaoProperties(
                new VotacaoProperties.Sessao(1),
                new VotacaoProperties.CpfClient("http://localhost"));
        service = new SessaoVotacaoService(repository, pautaService, props);
        when(pautaService.buscarPorId(1L)).thenReturn(new Pauta("p", "d"));
    }

    @Test
    void deveUsarDuracaoPadraoDeUmMinutoQuandoNaoInformada() {
        when(repository.findFirstByPautaIdOrderByDataAberturaDesc(1L)).thenReturn(Optional.empty());
        when(repository.save(any(SessaoVotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(1L, null);

        long minutos = Duration.between(sessao.getDataAbertura(), sessao.getDataEncerramento()).toMinutes();
        assertThat(minutos).isEqualTo(1);
    }

    @Test
    void deveUsarDuracaoInformada() {
        when(repository.findFirstByPautaIdOrderByDataAberturaDesc(1L)).thenReturn(Optional.empty());
        when(repository.save(any(SessaoVotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoVotacao sessao = service.abrir(1L, new AbrirSessaoRequest(15));

        long minutos = Duration.between(sessao.getDataAbertura(), sessao.getDataEncerramento()).toMinutes();
        assertThat(minutos).isEqualTo(15);
    }

    @Test
    void deveImpedirAberturaSeJaExisteSessaoAberta() {
        LocalDateTime agora = LocalDateTime.now();
        SessaoVotacao aberta = new SessaoVotacao(new Pauta("p", "d"),
                agora.minusMinutes(1), agora.plusMinutes(5));
        when(repository.findFirstByPautaIdOrderByDataAberturaDesc(1L)).thenReturn(Optional.of(aberta));

        assertThatThrownBy(() -> service.abrir(1L, null))
                .isInstanceOf(ConflictException.class);
    }
}
