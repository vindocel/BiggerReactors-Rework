# Manifolds e estabilização do trocador — 25/10/2026

## Alterações

- Cada coluna vertical conectada de manifolds compartilha uma escala de preenchimento. Frio desce do topo, quente sobe da base, sem margens vazias entre blocos. Os estoques continuam pertencendo ao reator; não foram criados inventários por manifold.
- Frio e quente ocupam espaços exclusivos. Se a soma dos dois buffers ultrapassa a capacidade visual de uma coluna, suas proporções são normalizadas para caber no mesmo tubo. Estoque ausente continua aparecendo como espaço vazio.
- O trocador preserva temperaturas restauradas ao remontar. Antes, `validationPassed()` substituía todas por temperatura ambiente, mesmo depois de `read()` carregar o calor salvo.
- Estatísticas informam temperaturas dos canais, variação do calor armazenado, aquecimento, limitação pelos níveis atuais e troca indireta quando não há faces de contato. O potencial é identificado como estimativa de regime estável com tanques de entrada cheios e saídas livres.
- Guia em português/inglês e dicas dos dois canais explicam contato por faces. Os circuitos mantêm seus fluidos separados mesmo encostados. Canto/aresta não conta; espaço entre os canais mantém somente a troca indireta pelo interior. A regra de montagem não foi restringida.
- A aparência dos canais do trocador não foi alterada nesta etapa; o autor indicou essa revisão como trabalho posterior.

## Investigação numérica

O autor confirmou um bloco de espaço entre os dois tipos de canal. Zero faces diretas é, portanto, coerente com essa montagem. Mais volume/canais não substitui área de contato direto.

`HeatExchangerCoolingGameTests.indirectExchangeWarmsUpAndConservesPrimaryVolume` usa 39 canais de cada tipo, nenhum contato direto, entrada de lava a 3.076 mB/t, saída livre e água disponível, com os tanques e corpos térmicos reais do mod. A geometria térmica da bancada reproduz o potencial indicado no print; não é uma cópia do mundo do jogador.

| Medida | Resultado |
| --- | ---: |
| Conversão média nos primeiros 2.000 ticks | 3.056,118 mB/t |
| Conversão média nos últimos 2.000 de 60.000 ticks | 3.076,000 mB/t |
| Potencial com entrada primária cheia | 3.232,361 mB/t |
| Estoque quente residual no fim | 92.522 mB |

Em todos os ticks, a entrada acumulada é igual ao retorno frio acumulado mais os estoques restantes. A diferença transitória não representa consumo/destruição do refrigerante. O resultado explica a estabilização observada; não prova que houve remontagem no mundo do autor. O reinício indevido de temperatura foi constatado separadamente no código e coberto por teste de estrutura montada.

## Validação

- Build e testes unitários passaram, incluindo coluna cheia sem lacunas, proporção 50/50 ao longo de três blocos e ausência de sobreposição dos fluidos.
- 30 GameTests passaram no servidor básico e no ambiente com integrações opcionais.
- Teste de multibloco verifica que calor primário, secundário e interno persistem após salvar, ler e remontar.
- Aparência final da coluna ainda precisa de conferência visual no cliente após reiniciar com o JAR atualizado.
- Sem commits ou mudanças nos controles de turbina.
