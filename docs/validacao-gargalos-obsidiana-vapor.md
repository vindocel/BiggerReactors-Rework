# Validação de gargalos: obsidiana/lava e vapor

Data: 26/10/2026. Simulações automatizadas, sem alterar as estruturas do mundo do autor.

## Resultado

Os dois experimentos reproduziram acúmulo por extração insuficiente, redução da produção até
acompanhar a extração, parada com saída cheia e recuperação após liberar a saída.
Não foi necessário modificar o código de funcionamento para obter esses resultados nesta tarefa.

| Experimento | Saída livre | Extração limitada | Produção estabilizada com limite | Bloqueio total | Após liberar a saída |
| --- | ---: | ---: | ---: | ---: | ---: |
| Reator: obsidiana → lava | 4.708,50 mB/t de lava | 2.354 mB/t | 2.354,00 mB/t | 0 mB/t | 4.708,50 mB/t |
| Trocador: água → vapor com lava no primário | 77.390,93 mB/t de vapor | 38.695 mB/t | 38.695,00 mB/t | 0 mB/t | 77.390,93 mB/t |

Números do ambiente básico. O ambiente com integrações opcionais confirmou o resultado, com
pequenas variações normais do reator. Os experimentos são separados: o trocador recebe lava
externamente para isolar sua saída de vapor. Não representam um único circuito em que o reator
desta tabela alimentaria toda a produção do trocador desta tabela.

## Método

Testes `measureRestrictedLavaExtractionFromObsidianReactor` e
`measureRestrictedSteamExtractionFromLavaExchanger`, em `OutputBottleneckStudyGameTests`.

- Mesma configuração e mesmos controles ao longo de cada experimento.
- Quatro etapas contínuas: saída livre, extração de 50% da produção inicial, extração zero e
  saída livre novamente. Nenhuma reinicialização entre etapas.
- Cada etapa dura 160.000 ticks; taxas médias dos últimos 20.000 ticks, após estabilização.
- JSON com médias e CSV com amostras a cada 200 ticks para acompanhar os estoques.
- Simulação real do reator, tanques e corpos térmicos de produção e controlador de acúmulo do
  trocador. A sequência térmica corresponde à do controlador do multibloco.
- Não é uma construção física no mundo nem um teste de um mod de canos específico.

## Reator com saída de lava restrita

Referência externa 6×5×6, interior 4×3×4, 24 blocos de combustível, 24 manifolds e 72 faces de
contato. Tanques de refrigerante de 240.000 mB cada. Planta alternada; sete rods a 42% e uma a
45%. Combustível e obsidiana repostos; rejeito retirado durante todo o teste.

| Etapa | Lava (mB/t) | Combustível (mB/t) | Lava retida ao final | Limite operacional médio pela saída |
| --- | ---: | ---: | ---: | ---: |
| Livre | 4.708,50 | 1,3403 | 0 mB | 100% |
| Extração de 2.354 mB/t | 2.354,00 | 0,8292 | 111.210 mB | 53,70% |
| Sem extração | 0 | 0 | 240.000 mB | 0% |
| Saída liberada | 4.708,50 | 1,3403 | 0 mB | 100% |

A produção caiu cerca de 50% e a queima cerca de 38%. Não precisam cair na mesma proporção:
temperatura, moderação e perdas térmicas continuam fazendo parte da física. A curva linear limita
a operação pela ocupação persistente; não fixa uma relação universal entre consumo e produção.

Verificações a cada tick: obsidiana fornecida menos convertida = estoque frio; lava produzida
menos extraída = estoque quente. Verificado também o teto de temperatura do reator.

## Trocador com saída de vapor restrita

Modelo térmico da referência 11×4×5: 19 canais por circuito, 18 faces de contato direto,
190.000 mB por tanque e ambiente a 293,15 K. Lava e água de entrada disponíveis; obsidiana fria
retirada livremente. Somente a extração de vapor muda entre etapas.

| Etapa | Vapor (mB/t) | Lava → obsidiana (mB/t) | Vapor retido ao final | Limite operacional médio pela saída |
| --- | ---: | ---: | ---: | ---: |
| Livre | 77.390,93 | 21.662,85 | 0 mB | 100% |
| Extração de 38.695 mB/t | 38.695,00 | 11.366,98 | 114.490 mB | 39,74% |
| Sem extração | 0 | 0 | 190.000 mB | 0% |
| Saída liberada | 77.390,93 | 21.662,85 | 0 mB | 100% |

Reter vapor reduziu também o processamento do primário. Com vapor totalmente bloqueado,
cessaram as duas conversões. Verificado a cada tick o balanço de fluidos de ambos os circuitos
e o balanço térmico: calor recebido do primário menos calor entregue ao secundário e ao ambiente
= variação do calor armazenado. Não houve criação ou perda não contabilizada de fluido/calor.

## Recuperação e limites

As taxas de recuperação foram medidas após estabilização. Recuperar o limite do controlador em
aproximadamente 1 segundo não significa reaquecer toda a máquina em 1 segundo após um bloqueio
prolongado. Temperaturas e demais condições continuam válidas.

Os testes anteriores cobrem saída cheia, persistência, frio sem penalidade e extração em lotes
de até 20 ticks antes/depois da produção. Turbina e modo sólido não foram alterados.

## Reprodução e resultados brutos

Comando: `gradlew.bat runGameTestServer runGameTestCompat --offline`.
Resultado: 46 GameTests obrigatórios aprovados em cada ambiente.

Em `run/gametest` e `run/gametest-compat` são gerados:

- `output-bottleneck-reactor_obsidian.json` e `.csv`.
- `output-bottleneck-exchanger_lava.json` e `.csv`.

Cópias desta validação: `output/cooling-study/bottleneck-core/` e `bottleneck-compat/`.

## Instruções consolidadas

Atualizado o AGENTS.md oficial. Corrigidas no próprio local ou marcadas como substituídas as
orientações antigas de somente planejamento, pacote indefinido, loader a confirmar e execução
ainda não autorizada. Reforçada a obrigação de atualizar e salvar as regras na mesma tarefa
em que um contrato aprovado mudar. Nenhum commit ou publicação foi feito.
