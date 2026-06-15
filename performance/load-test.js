/*
 * Teste de performance (Tarefa Bonus 2) com k6 - https://k6.io
 *
 * Simula centenas de milhares de votos concorrentes em uma unica pauta.
 *
 * Pre-requisitos:
 *   1. Subir a aplicacao (de preferencia com PostgreSQL):
 *        docker compose up -d
 *        mvn spring-boot:run -Dspring-boot.run.profiles=postgres
 *   2. Criar uma pauta e abrir uma sessao com duracao folgada (ex.: 60 min),
 *      e exportar o id da pauta:
 *        export PAUTA_ID=1
 *   3. Rodar:
 *        k6 run performance/load-test.js
 *
 * Observacao: o FakeCpfValidationClient rejeita ~30% dos CPFs (404) e marca
 * parte como UNABLE_TO_VOTE (422). Logo, 404/422 sao respostas ESPERADAS aqui;
 * o foco do teste e a latencia/vazao do registro de voto.
 */
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PAUTA_ID = __ENV.PAUTA_ID || '1';

export const options = {
  scenarios: {
    votacao_em_massa: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    // 95% das requisicoes abaixo de 300ms mesmo sob carga.
    http_req_duration: ['p(95)<300'],
  },
};

export default function () {
  // CPF "unico" por iteracao para exercitar a constraint de unicidade.
  const cpf = String(10000000000 + Math.floor(Math.random() * 89999999999)).slice(0, 11);
  const opcao = Math.random() < 0.5 ? 'SIM' : 'NAO';

  const res = http.post(
    `${BASE_URL}/api/v1/pautas/${PAUTA_ID}/votos`,
    JSON.stringify({ associadoId: cpf, opcao }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'resposta tratada (201/404/422/409)': (r) => [201, 404, 422, 409].includes(r.status),
  });
}
