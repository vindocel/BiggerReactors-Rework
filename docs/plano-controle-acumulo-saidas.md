# Plano de controle por acúmulo nas saídas

Data: 26/10/2026  
Estado: implementação autorizada pelo autor em 26/10/2026; controle e testes automatizados implementados.  
Escopo: reator no modo líquido e trocador de calor.

## 1. Objetivo

Reduzir gradualmente a operação quando houver acúmulo persistente do produto quente por
extração insuficiente: refrigerante quente/vapor na saída do reator e vapor na saída do trocador.
A redução deve começar antes da lotação completa e acompanhar o estoque retido além do
ciclo normal de extração. Ao liberar a saída, a máquina poderá recuperar sua taxa de processamento.

No reator líquido, os produtos são vapor ou refrigerante quente, conforme o perfil utilizado.
O controle reduz a conversão de refrigerante frio em quente e a queima de combustível associada.
No trocador, limita a conversão de refrigerante quente em frio e de água em vapor.

“Redução de desempenho” significa menor taxa de processamento. Não significa adicionar
desperdício de combustível nem diminuir artificialmente o calor obtido por unidade consumida.

## 2. Limites do escopo

| Sistema | Tratamento neste plano |
| --- | --- |
| Reator no modo líquido | Aplicar o controle à produção de vapor/refrigerante quente e à queima associada. |
| Reator no modo sólido | Fora do escopo. Não alterar geração direta de FE, moderação sólida ou queima por esta regra de acúmulo de fluidos. |
| Trocador de calor | Aplicar o controle às duas conversões, tendo o vapor acumulado como causa da redução gradual. |
| Turbina | Fora do escopo. Preservar controles, limites estruturais, vazão, rotor, geração e descarte. |

Não alterar a progressão dos refrigerantes, suas capacidades térmicas ou o dimensionamento
das estruturas como consequência desta implementação.

## 3. Regra comum de acúmulo

1. Medir o produto quente/vapor retido na saída após considerar o ciclo normal de extração.
2. Distinguir acúmulo persistente de produto recém-gerado que será retirado normalmente.
3. Determinar um limite de operação que diminua conforme a ocupação persistente aumenta.
4. Combinar esse limite com disponibilidade de entrada, controles manuais e capacidade térmica.
5. Com a saída sem espaço real para novo produto, impedir conversões que ultrapassariam sua capacidade.
6. Recuperar a operação conforme o acúmulo diminuir, sem ultrapassar os limites da configuração.

Com a saída livre, o acúmulo não deve reduzir a capacidade disponível. As outras limitações
continuam válidas, incluindo falta de refrigerante, falta de água e contatos térmicos insuficientes.

A medição utiliza os tanques reais. O preenchimento visual dos tubos representa a proporção
entre fluidos e não serve como medida de ocupação dos buffers.

### Produção em trânsito não é acúmulo

Produzir um lote e mantê-lo por alguns ticks até o cano retirá-lo faz parte da circulação normal.
Esse estoque transitório não deve reduzir a queima nem a conversão se o transporte escoa toda
a produção dentro de seu ciclo normal, mesmo que a extração aconteça em lotes ou após a máquina
processar seu tick. Uma leitura instantânea logo após produzir não basta para aplicar a penalidade.

A implementação deverá observar produção, extração e estoque residual em uma janela coerente
com o ciclo de transferência. O teste decisivo é: extração suficiente para retirar toda a produção
deve manter a operação máxima disponível, sem oscilações artificiais causadas pela ordem dos ticks.
Estoque retido por falta real de extração deve ser detectado mesmo que deixe de crescer depois
que a própria redução de produção equilibrar a vazão. Não usar apenas a tendência de crescimento.

A tolerância temporal não permite transbordamento, descarte automático ou conversão sem espaço.
O autor aprovou uma janela de 20 ticks (1 segundo) e retomada de até 5 pontos percentuais por tick.

## 4. Reator no modo líquido

### Conversões abrangidas

| Entrada | Produto cuja acumulação limita a operação |
| --- | --- |
| Água | Vapor |
| Obsidiana líquida | Lava |
| Sódio líquido frio | Sódio líquido superaquecido |
| Cryo Fuel | Cryo Fuel aquecido identificado |

As integrações opcionais continuam condicionadas à presença dos respectivos mods.

### Comportamento esperado

- Produto quente acumulando: reduzir gradualmente a conversão e a queima de combustível.
- Produto quente sendo extraído suficientemente: permitir o máximo da configuração e dos controles.
- Saída quente cheia: zerar novas conversões e a queima de combustível.
- Refrigerante frio ausente: manter a limitação já existente por falta de entrada.
- Refrigerante frio abundante ou tanque de entrada cheio: não aplicar penalidade por excesso.
  A disponibilidade de frio pode favorecer a troca até o máximo da montagem; não cria capacidade ilimitada.
- Produto quente produzido e integralmente retirado no ciclo normal de transporte: não penalizar.
- Preservar o limite manual das rods; a automação pode reduzir a operação, mas não ultrapassá-lo.

O limite precisa alcançar tanto a reação quanto a conversão. Apenas reduzir a queima não
garante limitar a conversão, porque o calor anteriormente armazenado pode continuar alimentando-a.
O calor retido deve permanecer contabilizado, sem ser apagado artificialmente.

## 5. Trocador de calor

### Circuitos

- Primário: recebe refrigerante quente e produz refrigerante frio.
- Secundário: recebe água e produz vapor.

### Estoque que causa a redução gradual

- Tanque de vapor produzido pelo secundário.
- O refrigerante quente no primário é entrada, não produto de saída: sua abundância não é causa
  de penalidade por acúmulo. O papel do tanque depende da máquina, não apenas da temperatura do fluido.

### Comportamento esperado

- Vapor persistentemente acumulado: reduzir a produção de vapor e o processamento de refrigerante quente.
- Vapor produzido e integralmente retirado no ciclo normal: não aplicar redução por acúmulo.
- Água abundante ou tanque de entrada de água cheio: não aplicar penalidade por excesso.
- Refrigerante frio armazenado: não aplicar penalidade gradual pela porcentagem desse tanque.
- Saída de vapor cheia sem extração disponível: bloquear novas conversões nos dois circuitos.
- Falta de água ou de refrigerante quente continua limitando normalmente o funcionamento.

### Capacidade física da saída fria

Não penalizar gradualmente o estoque frio não significa produzir além da capacidade do tanque.
Se a saída de refrigerante frio ficar totalmente cheia, a conversão de quente em frio permanece
limitada ao espaço disponível. Isso é um limite físico de armazenamento, distinto da nova
penalidade por acúmulo de vapor. Não impor, por esse motivo, uma penalidade gradual comum aos
dois circuitos. A evaporação ainda pode aproveitar calor já armazenado, respeitando o balanço térmico.

Integrar esse controle ao balanço térmico existente. Não consumir fluido sem registrar o
produto correspondente, criar calor ou aplicar a mesma redução repetidamente em cada etapa.
As temperaturas e o calor armazenado continuam sendo acompanhados mesmo com a conversão bloqueada.

## 6. Propagação natural pelo circuito

1. A turbina não acompanha a produção de vapor recebida.
2. A extração do trocador fica limitada e o vapor começa a se acumular nele.
3. O trocador reduz o processamento e o retorno de refrigerante frio.
4. O refrigerante quente passa a se acumular no reator.
5. O reator reduz a conversão e a queima.

Cada máquina reage aos seus próprios estoques, sem consultar ou controlar máquinas remotas.
A turbina continua sendo ajustada pelo jogador por meio da vazão e do dimensionamento existentes.

## 7. Interface e estatísticas

### Reator líquido

- Mostrar a ocupação da saída quente e a limitação por acúmulo.
- Manter queima atual, conversão atual e potencial da configuração.
- Indicar quando a saída quente está limitando ou bloqueando a operação.
- Usar os nomes do par de fluidos reconhecido, sem tratar todos os refrigerantes como água.

### Trocador

- Mostrar ocupação dos tanques e identificar vapor persistentemente acumulado como causa da redução gradual.
- Distinguir essa condição de falta de entrada ou de saída fria fisicamente sem espaço.
- Preservar a distinção entre capacidade potencial e conversão atual.

O potencial corresponde à configuração em condições ideais. O acúmulo limita a operação atual,
sem substituir o potencial por um valor reduzido. Seguir o padrão vigente das estatísticas do mod.

## 8. Texto proposto para o guia

Esta explicação é destinada ao jogador; não constitui uma nova mecânica ou um novo requisito
de implementação além dos controles descritos acima.

> O reator possui proteção automática para manter a estabilidade da operação. Quando o vapor
> ou o refrigerante quente se acumula na saída, o sistema reduz a queima de combustível e a
> conversão do refrigerante, evitando condições críticas. Se a saída ficar completamente cheia,
> a produção é interrompida até que haja espaço disponível.
>
> No trocador de calor, o vapor retido na saída reduz a troca térmica e o retorno de refrigerante
> frio. Mantenha o abastecimento e a extração adequados para aproveitar a capacidade da montagem.
> Tanques de entrada cheios não causam essa redução. O fluido que está apenas aguardando a
> retirada no ciclo normal dos canos também não é considerado acúmulo persistente.

A linguagem apresenta a redução como proteção automática, sem sugerir uma ameaça de explosão
nem responsabilizar o jogador com mensagens depreciativas.

## 9. Parâmetros aprovados na autorização de implementação

As pendências anteriores foram resolvidas pelo autor em 26/10/2026:

- **Curva:** limite linear = 1 − ocupação persistente da saída. Exemplo: 50% retidos permite
  até 50% da operação, combinada com as demais limitações.
- **Detecção:** menor ocupação observada durante os últimos 20 ticks (1 segundo), medindo antes
  da produção e registrando também os níveis após extrações reais. Simulações de transferência
  não modificam o histórico. O menor estoque de cada tick entra na janela.
- **Lotes:** se a saída é esvaziada no ciclo normal de até 20 ticks, o lote transitório não cria
  penalidade. Um estoque residual persistente continua sendo considerado mesmo quando deixa de crescer.
- **Retomada:** até 5 pontos percentuais por tick, chegando de 0 a 100% em aproximadamente
  1 segundo quando as demais condições permitirem. A proteção térmica ainda pode limitar a queima.
- **Saída cheia:** bloqueio imediato, sem tolerância para ultrapassar a capacidade física.
- **Persistência:** salvar histórico e limite para não perder o controle ao recarregar a máquina.

Os estados finais estão definidos: saída livre não impõe redução por acúmulo; saída cheia
bloqueia novas conversões; no reator líquido, também zera a queima.

## 10. Validação prevista

- Comparar a mesma configuração em vários níveis de ocupação, controlando as demais condições.
- Verificar redução gradual conforme a curva que vier a ser aprovada.
- Confirmar conversão e queima zeradas no reator líquido com saída cheia.
- Confirmar redução e bloqueio conjunto do trocador por vapor persistentemente acumulado/saída de vapor cheia.
- Confirmar ausência de penalidade gradual com água de entrada cheia, refrigerante frio de entrada
  cheio no reator e estoque de refrigerante frio na saída do trocador.
- Confirmar limite físico da saída fria cheia, sem transbordamento nem descarte automático.
- Verificar retomada após liberar a extração e estabilidade com vazão constante insuficiente.
- Testar com calor previamente armazenado, sem criar ou apagar energia térmica artificialmente.
- Verificar conservação de volumes e funcionamento de todos os refrigerantes disponíveis.
- Confirmar preservação dos controles das rods, do modo sólido e do comportamento da turbina.
- Confirmar que o controle não reduz a operação quando toda a produção é escoada, incluindo
  canos que extraem depois da produção e canos que transferem em lotes a cada vários ticks.
- Confirmar detecção de acúmulo persistente tanto crescente quanto estabilizado pela redução da produção.

## 11. Implementação e validação

- `OutputBackpressure`: detector comum de estoque persistente, curva linear, retomada e estado salvo.
- Reator: controle aplicado à queima e à transferência de calor para conversão do refrigerante;
  cálculo do teto térmico considera a limitação da saída.
- Trocador: controle compartilhado entre as duas conversões, alimentado exclusivamente pela saída de vapor.
- Limites físicos de tanques permanecem ativos. Nenhum fluido ou calor é descartado pelo controlador.
- Estatísticas: ocupação atual, ocupação persistente, operação permitida e motivo da limitação/retomada.
- Guia em português e inglês: proteção automática, circulação transitória, saídas, estoques frios e retomada.
- Turbina e modo sólido preservados. Nenhum commit ou alteração de versão. Não foram editados arquivos
  de mundo nem estruturas; a conferência no cliente usou o mundo existente e movimentou o jogador.

Validação automatizada: `gradlew.bat build runGameTestServer runGameTestCompat --offline`.
35 testes unitários e, após acrescentar os experimentos de gargalo, 46 GameTests obrigatórios em cada ambiente aprovados. Os testes de integrações
opcionais são executados com os respectivos mods instalados no ambiente compat.

Os testes novos cobrem transferências antes/depois da produção e em lotes de 1, 10 e 20 ticks,
conservação, bloqueio com calor armazenado, retomada, persistência e estoque frio sem penalidade.
As comparações existentes também verificam o teto térmico e a progressão dos refrigerantes.
Cliente `runClient` aberto no diretório `run`: conferida a explicação em português do trocador no guia,
com paginação correta e sem crash. A inspeção visual das novas linhas nas estatísticas de um reator
líquido/trocador em operação permanece como verificação manual; não foi apresentada como executada.

Experimentos adicionais de extração limitada à metade, bloqueio e recuperação documentados em
`docs/validacao-gargalos-obsidiana-vapor.md`, com dados brutos e verificações de balanço por tick.
