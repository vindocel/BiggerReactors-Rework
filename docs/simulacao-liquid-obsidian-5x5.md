# Estudo numérico: reator 5×5×5 com Liquid Obsidian

Data: 25/10/2026. Estado: referência do código atual para orientar o rework; não é resultado da futura redução automática de queima.

## Resultado principal

Na montagem descrita abaixo, com estoque cheio, reposição contínua e rods totalmente liberadas, o reator atual produz aproximadamente **66,31 B/s de lava** com alimentação/retirada ideais. Em circuito primário fechado, um trocador **4×4×5 externo**, com **3 canais condensadores e 3 evaporadores**, sustenta aproximadamente **64,21 B/s** de circulação e **220,67 B/s de vapor**. Um **4×4×6**, com 4 canais de cada tipo, sustenta **65,03 B/s** e **224,21 B/s de vapor**, com maior capacidade disponível no trocador.

Esses números dependem de geometria, estoque e parâmetros atuais. Não representam o máximo de todos os arranjos possíveis de um reator 5×5, nem o máximo do futuro rework: falta calibrar seu limite térmico. A simulação atual atinge temperaturas de combustível muito elevadas, registradas abaixo, porque ainda não aplica a nova proteção.

Unidades: **1 B = 1.000 mB**, um balde. Todos os segundos correspondem a 20 ticks de jogo. Capacidades são volumes armazenáveis; vazões são volumes por segundo.

## 1. O que foi efetivamente executado

- Execução numérica fora do jogo, usando as classes atuais de configuração, geometria e simulação CPU do reator, calor e tanques de transição. Essas classes foram recompiladas dos fontes em uma pasta separada, sem mudar o código do mod.
- Reprodução da geometria térmica e da ordem de operações de `HeatExchangerMultiblockController` em uma bancada separada; os tanques e a transferência de calor usam as classes do mod.
- Configuração padrão de `Config.java`; ambiente a 293,15 K (20 °C); propriedades de ar e Liquid Obsidian correspondentes aos JSON5 atuais.
- 20 cenários: 5 capacidades de trocador com alimentação ideal; 3 montagens de reator com circulação ideal; 12 combinações de reator e trocador em circuito primário fechado.
- Cada cenário: 160.000 ticks de estabilização e 20.000 ticks de coleta. São 2 h 30 min de tempo de jogo simulado por cenário, com médias dos últimos 16 min 40 s.
- Reposição de urânio e retirada de rejeito a cada tick, preservando o estoque cheio. Todos os ajustes de inserção das rods a 0%, liberando a reação.
- Transferências entre máquinas sem limite de vazão e sem atraso de canos. Água do trocador reposta e vapor retirado continuamente. Não foi simulada uma turbina, custo de bombas, chunk loading ou o transporte dentro de mods externos.
- No circuito primário fechado, o único estoque inicial é o frio do reator. Não há reposição de obsidiana ou lava externa. O volume se conserva entre os tanques do reator e do trocador.

A checagem automática confirmou conservação de inventário nos 12 circuitos fechados, igualdade das vazões médias de ida/retorno dentro de 0,02 mB/s e energia do vapor não superior ao calor recebido do refrigerante nos cenários medidos.

Os testes foram numéricos. A formação física dessas máquinas no mundo e o percurso dos canos não foram executados nesta tarefa. Variações pequenas entre execuções são esperadas porque a simulação atual embaralha a ordem de amostragem das barras.

## 2. Montagem de referência do reator

Dimensões externas: **5×5×5**. Interior: **3×3×3**.

Repetir nos três níveis internos:

```text
S S S S S
S F M F S
S M F M S
S F M F S
S S S S S
```

- `S`: revestimento externo.
- `F`: barra de combustível.
- `M`: coolant manifold.

São **5 colunas de combustível**, 3 blocos por coluna: **15 blocos de combustível**, com 5 controles no teto. Há **12 manifolds**, com **36 faces laterais de contato com combustível**. Os manifolds também alcançam o revestimento sólido; não substituir seus pontos de conexão ao exterior por vidro.

O estoque de combustível dessa montagem tem capacidade de **60.000 mB**, equivalente a **60 lingotes** pela configuração atual. Isso é estoque, não queima por segundo. No cenário acoplado ao trocador 4×4×5, a queima medida foi **39,04 mB/s**, aproximadamente **1,95 mB/t** ou **um lingote a cada 25,6 segundos**.

Esta é uma montagem de referência com bastante combustível e contato direto. Não foi feita busca exaustiva para encontrar o melhor arranjo de barras dentro do volume.

## 3. Resultado do reator com circulação ideal

Retirando toda a lava e mantendo o estoque frio cheio a cada tick:

| Medida | Resultado aproximado |
| --- | ---: |
| Combustível consumido | 38,40 mB/s |
| Liquid Obsidian convertido | 66.310,66 mB/s = 66,31 B/s |
| Lava produzida | 66.310,66 mB/s = 66,31 B/s |
| Calor entregue ao refrigerante | 49.733 FE térmicos/t |
| Temperatura do combustível | 2.676 °C |
| Temperatura do conjunto do reator | 1.535 °C |

Não confundir os 49.733 FE térmicos/t com energia elétrica exportada: ainda há transferência pelo trocador, vapor e conversão na turbina.

É o teto medido dessa montagem sob as condições de abastecimento escolhidas. Nenhum limite térmico novo foi inventado para reduzir esse resultado.

## 4. Trocadores avaliados

Todos usam duas linhas retas paralelas, de tipos diferentes, encostadas lateralmente ao longo de todo o comprimento. Cada linha tem uma entrada e uma saída próprias. São quatro portas de fluido no total, além do terminal.

Para o **4×4×5**, tomando coordenadas externas de zero até o limite:

- Linha condensadora: `(1,1,z)`, com `z = 1, 2, 3`.
- Linha evaporadora: `(2,1,z)`, com `z = 1, 2, 3`.
- As quatro portas ficam nas extremidades `z = 0` e `z = 4`, alinhadas a cada linha.
- O restante do interior fica livre. Preservar armação/revestimento e colocar o terminal em outra posição válida da face.

```text
entrada lava       C C C       saída Liquid Obsidian
entrada água       E E E       saída vapor
```

`C` e `E` são linhas lado a lado, com **3 faces de contato entre elas**. Não são dois circuitos misturados.

No 4×4×6, estender as duas linhas para quatro blocos e colocar as portas nas novas extremidades. Um trocador maior com canais separados pode ser pior que este: a área real de contato e o comprimento funcional importam mais que o volume externo sozinho.

### Capacidade com alimentação ideal de lava e água

Esta é a capacidade térmica medida mantendo as entradas cheias e as saídas livres, nas condições da bancada. Não é uma vazão universal independente da ocupação dos tanques.

| Tamanho externo | Canais C + E | Faces C/E | Lava resfriada | Vapor gerado |
| --- | ---: | ---: | ---: | ---: |
| 4×4×4 | 2 + 2 | 2 | 47,86 B/s | 162,67 B/s |
| 4×4×5 | 3 + 3 | 3 | 71,27 B/s | 245,24 B/s |
| 4×4×6 | 4 + 4 | 4 | 94,65 B/s | 327,88 B/s |
| 4×4×7 | 5 + 5 | 5 | 118,01 B/s | 410,56 B/s |
| 4×4×8 | 6 + 6 | 6 | 141,36 B/s | 493,27 B/s |

Para a demanda ideal do reator, aproximadamente 66,31 B/s, o 4×4×4 é insuficiente. O 4×4×5 passa no dimensionamento nominal com apenas cerca de 7,5% de folga. O 4×4×6 tem cerca de 42,7% de folga nessa comparação. Folga térmica não significa produzir mais vapor sem calor adicional.

## 5. Reator e trocador ligados em circuito fechado

O reator de 15 barras é o mesmo em todas as linhas; os resultados abaixo incluem a influência da ocupação dos tanques na eficiência atual da troca.

| Trocador | Queima de combustível | Obsidiana entrando / lava saindo | Obsidiana retornando | Água consumida / vapor gerado |
| --- | ---: | ---: | ---: | ---: |
| 4×4×4 | 45,09 mB/s | 47,86 B/s | 47,86 B/s | 162,67 B/s |
| 4×4×5 | 39,04 mB/s | 64,21 B/s | 64,21 B/s | 220,67 B/s |
| 4×4×6 | 38,78 mB/s | 65,03 B/s | 65,03 B/s | 224,21 B/s |
| 4×4×8 | 38,66 mB/s | 65,46 B/s | 65,46 B/s | 225,90 B/s |

O 4×4×5 fecha o ciclo numericamente: entrada e retorno se equilibram e o inventário primário permanece em **270.000 mB**, sem reposição. O 4×4×6 oferece mais margem de processamento, mas o ganho de produção é pequeno porque o limite passa a ficar principalmente no reator.

Não igualar a capacidade de alimentação ideal do trocador ao resultado do sistema conectado. No circuito real simulado, a quantidade disponível em cada tanque é finita e muda o coeficiente efetivo de troca; por isso o retorno é próximo, mas inferior aos 66,31 B/s do reator abastecido idealmente.

No código atual, o trocador 4×4×4 provoca acúmulo de cerca de **117,87 B de lava no reator**, temperatura de combustível próxima de **3.046 °C** e queima maior, embora produza menos vapor. Ele encontra outro equilíbrio, mais quente e menos eficiente; **não é a redução automática futura funcionando**. No rework, esse gargalo deverá reduzir a queima solicitada, em vez de ser compensado com aquecimento e desperdício de combustível.

Mesmo os casos 4×4×5 e 4×4×6 têm combustível perto de 2.700 °C no modelo atual. Se a calibração do rework adotar uma faixa térmica inferior, a queima e as vazões máximas serão diferentes. Esses resultados servem para dimensionar uma referência, não para fixar as temperaturas finais.

## 6. Balanço de calor do exemplo 4×4×5

As transições padrão atuais transportam:

- Liquid Obsidian → lava: **15 FE térmicos por mB**.
- Água → vapor: **4 FE térmicos por mB**.

Portanto, antes das perdas, cada 1 mB de lava resfriada pode sustentar no máximo **3,75 mB de vapor** pelas receitas atuais.

No circuito 4×4×5:

```text
64.207,11 mB/s de lava × 15 ≈ 963.107 FE térmicos/s
963.107 ÷ 4 ≈ 240.777 mB/s de vapor antes das perdas

Vapor medido: 220.673,62 mB/s
Calor no vapor: 882.694 FE térmicos/s
Recuperação no trocador: aproximadamente 91,7%
```

O restante é principalmente a perda ambiental do modelo, com pequenas oscilações/arredondamentos e variações da energia armazenada. Esse resultado não prova ainda que obsidiana supera água ou sólidos em FE/combustível: falta executar a comparação completa com a turbina e com os novos parâmetros.

## 7. Buffers e vazões para construir o exemplo

Na configuração atual, o reator tem **270 B de capacidade por lado**; o 4×4×5 tem **30 B por tanque**, e o 4×4×6 tem **40 B por tanque**. Existem dois tanques no primário e dois no circuito de água/vapor do trocador.

A bancada começou com 270 B de obsidiana no reator, sem estoque primário nos canos. Esse volume foi redistribuído entre as máquinas durante o funcionamento. Uma montagem no jogo também precisa carregar os volumes dos canos e eventuais tanques externos.

Essas são as capacidades existentes, que contam barras e manifolds no reservatório do reator. O plano prevê revisar essa regra para associar o armazenamento aos manifolds; os 270 B não são uma capacidade já aprovada para o rework.

Para reproduzir aproximadamente o cenário 4×4×6, dimensionar as conexões para pelo menos:

| Conexão | Vazão de referência |
| --- | ---: |
| Lava: reator → trocador | 65.028 mB/s ≈ 3.251 mB/t |
| Obsidiana: trocador → reator | 65.028 mB/s ≈ 3.251 mB/t |
| Água: fonte externa/retorno → trocador | 224.206 mB/s ≈ 11.210 mB/t |
| Vapor: trocador → turbina | 224.206 mB/s ≈ 11.210 mB/t |

Os números são vazões médias; uma montagem prática precisa tolerar picos e latência. A bancada não mede limite específico de qualquer cano externo.

## 8. Circuito aberto ou fechado da água

As funções atuais da turbina devem ser preservadas: controle de vazão, bobinas, redstone e modos de descarte. Na implementação atual, `OVERFLOW`, `ALL` e `CLOSED` controlam o tratamento da **água condensada**, não um bônus de vapor.

- **Aberto:** fonte externa fornece água ao trocador; vapor alimenta a turbina; água condensada pode ser descartada conforme a opção escolhida. Para o cenário 4×4×6, a fonte externa precisa sustentar cerca de 224,21 B/s.
- **Fechado:** água recuperada na turbina volta ao trocador; o usuário dimensiona retorno e carga inicial, com reposição apenas quando houver perdas/descarte.
- O circuito primário obsidiana/lava pode permanecer fechado nas duas opções. O rework não obriga fechar o circuito secundário da água.
- Fonte infinita de água não é fonte infinita de calor. Sem lava quente produzida no reator ou outro calor de entrada, o trocador não sustenta a produção de vapor.

No rework, respeitar descarte configurado não dispensa reação a gargalos reais: se a turbina parar de aceitar vapor e a água/vapor saturar o trocador, o retorno de obsidiana pode cair e o reator deve reduzir a queima. Não inferir que o descarte de água resolve todo bloqueio de vapor.

## 9. Artefatos e limites

- Resultados brutos e invariantes: `output/cooling-study/results.json`.
- Bancada reproduzível e recompilação isolada: `output/cooling-study/CoolingStudy.java` e `run-study.ps1`.
- Hashes dos fontes usados: `output/cooling-study/source-hashes.json`.

Os auxiliares ficam em `output/`, fora do gameplay; este relatório preserva no projeto os parâmetros e conclusões. Não houve alteração de fontes do mod, parâmetros de geração ou arquivos de mundo, nem teste de renderização ou montagem dentro do jogo.

**Próximo uso destes dados no plano:** comparar sólidos, água e obsidiana com turbina dimensionada; propor faixa térmica e controle automático; repetir esta bancada com os parâmetros aprovados. Não transportar os máximos atuais para a interface futura sem essa etapa.
