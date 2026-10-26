# Correção da progressão do sódio — 26/10/2026

O autor confirmou que trocar somente o refrigerante, mantendo o reator, o trocador e os controles,
deve fazer o sódio superar obsidiana e Cryo Fuel. Não aumentar o trocador para cumprir a progressão.

## Causa e alteração

O perfil anterior usava 473,15 K para a transferência do sódio quente, apenas 100 K acima da água.
Isso limitava o trocador mesmo com a recuperação de 95% no reator. O sódio quente acumulava no
primário, faltava retorno frio e o controle térmico podia reduzir a queima.

O perfil passa a usar 1273,15 K como temperatura de troca no modelo do Rework. É um parâmetro
de balanceamento do circuito, não uma afirmação sobre propriedades reais do sódio ou alteração
das máquinas do Mekanism. Mantidos: calor por mB = 40, recuperação = 95%, condutividades = 4,
par nativo de fluidos, capacidades, combustível e controles das máquinas.

## Comparação automatizada

Teste `coolantProgressionUsesTheSameExchanger`, com os perfis reais dos mods opcionais:

- Reator externo 6×5×6, 24 blocos de combustível, 24 manifolds e 72 faces produtivas.
- Mesma planta alternada da comparação anterior, com três blocos de altura interna.
- Mesmas inserções: sete rods a 42% e uma a 45%, em todos os casos.
- Trocador representado pelo modelo de produção: 19 canais por circuito, 18 faces diretas;
  geometria térmica de referência 11×4×5. Não é uma leitura automática da montagem atual do mundo.
- Primário fechado com 240.000 mB totais; conservação verificada a cada tick. Água secundária
  disponível, retirada livre de vapor e transporte ideal. Sem turbina na medição.
- Aquecimento de 160.000 ticks e duas janelas de 20.000 ticks; diferença entre janelas <0,5%.

Valores arredondados; pequenas variações entre execuções são normais na simulação:

| Refrigerante | Vapor (mB/t) | Combustível (mB/t) | Vapor por mB de combustível |
| --- | ---: | ---: | ---: |
| Água direta | 9.316 | 1,616 | 5.765 |
| Obsidiana em circuito | 16.190 | 1,352 | 11.978 |
| Cryo Fuel em circuito | 15.192 | 1,157 | 13.133 |
| Sódio anterior | 10.326 | 1,930 | 5.351 |
| Sódio corrigido | 25.779 | 1,128 | 22.856 |

O sódio corrigido supera os dois circuitos em vapor/t e vapor/combustível. Os demais perfis não
foram alterados; esta correção não define uma nova proporção obrigatória entre obsidiana e Cryo.
Os resultados não prometem valores idênticos aos prints: inserções, transporte e condições de
operação precisam coincidir. Relatório bruto: `run/gametest-compat/coolant-progression.json`.

## Saves e visualizadores

O carregamento reconhece exclusivamente a assinatura anterior exata do sódio e adota o perfil
corrigido, preservando os volumes dos dois lados e o calor fracionário. Mudanças arbitrárias nos
demais parâmetros continuam em quarentena. Teste de regressão cobre os dois sentidos da troca
e a recusa de assinaturas desconhecidas. Nenhum arquivo do mundo do autor foi editado.

JEI/EMI deixaram de chamar o calor por mB de FE/mB. Mostram temperatura de troca e destino do
produto: vapor → turbina; demais quentes → trocador. A geração elétrica continua sendo da turbina.

Validação: `gradlew.bat build runGameTestServer runGameTestCompat --offline`, com 39 GameTests
obrigatórios aprovados em cada ambiente. Sem mods opcionais, os testes específicos de integrações
são dispensados; no ambiente compat, Mekanism e Ad Astra reais estão presentes.
