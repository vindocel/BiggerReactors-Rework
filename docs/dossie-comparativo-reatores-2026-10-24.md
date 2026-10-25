# Dossiê técnico comparativo — BiggerReactors-Rework

> **Diretriz posterior do autor (24/10/2026):** a execução passa a seguir o [plano consolidado](C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/docs/plano-implementacao-consolidado.md). Revalidar cada bug no checkout atual antes de corrigir; pular os já resolvidos ou não aplicáveis. O guia usará a tecnologia própria do DFM 1.21.1, sem Patchouli ou biblioteca externa, conforme `docs/planejamento-guia.md`. Essa decisão substitui as alternativas ainda abertas neste dossiê; o restante permanece como registro da análise.


**Data:** 24/10/2026. **Alvo:** Minecraft 1.21.1, NeoForge, Java 21.  
**Projetos de referência:** ExtremeReactors2 e Bigger-Reactors-Revived.  
**Finalidade:** fundamentar um futuro plano de implementação. Este trabalho entrega análise e documentação; não incorpora implementações dos outros mods.

## 1. Parecer executivo

O Rework já é uma base própria e funcional. **A melhor estratégia é preservar seu núcleo, corrigir os defeitos localizados e incorporar recursos selecionados.** Substituir os controladores ou importar um dos forks inteiro perderia correções próprias e reintroduziria dependências que o Rework já eliminou.

As maiores oportunidades são:

1. **Correções e testes antes de novos sistemas:** coordenada errada em mensagem de validação; comando de reverter da porta de redstone sem efeito; validação incompleta dos dados enviados à porta; amostragem vertical da simulação fracionada e transição de desligamento da simulação assíncrona.
2. **Do Revived:** provedores de geração de dados, Data Maps nativos do NeoForge, integração opcional com KubeJS, livro com estruturas projetáveis, comando de montagem para desenvolvimento e item de saída de combustível configurável.
3. **Do Extreme Reactors:** sensores de redstone para turbinas, sincronização de RPM por mudança relevante, instrumentação de desempenho, separação de interfaces de leitura/escrita, combustíveis e reações extensíveis e receitas próprias para processamento.
4. **Preservar as vantagens do Rework:** funcionamento sem bibliotecas externas de multibloco/renderização, reprocessador com configuração por face, rede de menus executada na thread do servidor, integração EMI/Jade, configuração explícita da simulação e verificação automatizada de montagem/conteúdo.
5. **Não tratar conteúdo adicional como correção:** tiers, Energizer, Fluidizer, novos materiais e reprocessador multibloco do Extreme são expansões de projeto. Podem ser adaptados, mas mudam progressão, balanceamento e custo de manutenção.

Também há um resultado importante em sentido inverso: **o Revived conserva problemas que o Rework já corrigiu.** O exemplo mais claro é o tanque de água do reprocessador: na referência, grava-se `fluidTank` e lê-se `fluidStorage`. Portanto, o reprocessador local não deve ser substituído pelo dele. [L12] [V08]

### 1.1 Quadro de decisão

| Área | Referência mais útil | Decisão sugerida | Motivo |
| --- | --- | --- | --- |
| Base autônoma e dependências | Rework | Preservar | Inicialização e teste sem Phosphophyllite, Quartz ou ZeroCore externos. |
| Reprocessador atual | Rework | Preservar e testar | Persistência corrigida, entrada de energia e automação por face. |
| Dados e ferramentas de manutenção | Revived | Adaptar gradualmente | Datagen e Data Maps reduzem infraestrutura própria para conteúdo. |
| Guia dentro do jogo | Revived e Extreme | Aproveitar estrutura editorial | Ambos possuem conteúdo executável de livro; o Rework possui planejamento. |
| Redstone de turbina | Extreme | Implementar sobre a simulação local | Lacuna funcional real, sem exigir troca de física. |
| Sincronização visual | Extreme | Adaptar o algoritmo | RPM atualizado por diferença e intervalo; benefício deve ser medido. |
| Combustíveis extensíveis | Extreme | Projeto posterior | Exige nova representação de combustível, resíduos e persistência. |
| Simulação nuclear | Rework + auditoria | Corrigir e medir | Os três modelos não são intercambiáveis sem alterar resultados. |
| Renderização | Rework | Preservar e medir | Remove Quartz; desempenho comparativo ainda não foi medido. |
| Trocador de calor | Rework/Revived | Preservar | As diferenças observadas no controlador são principalmente de infraestrutura. |
| Publicação | Rework | Preservar | CI separado da publicação, validação de versão e GameTests. |

## 2. Escopo, versões e força das evidências

### 2.1 Fotografias utilizadas

| Projeto | Revisão analisada | Versão/plataforma confirmada |
| --- | --- | --- |
| Rework | `3542de79897022bafc73d2c527310327724d910f` **mais alterações locais existentes** | 0.1.0; Minecraft 1.21.1; NeoForge 21.1.264; Java 21. |
| Revived | branch `1.21.1`, commit `d20a6bef5352308638631dbb1bfa6792958ac3b4` | 1.0.2; Minecraft 1.21.1; NeoForge 21.1.240; Java 21. |
| Extreme | branch `1.21`, commit `f6cc65140a8abbfc87137f5c54fc641085f66048` | 2.4.27; Minecraft 1.21.1; NeoForge 21.1.152; ZeroCore 2.4.18. |

**A branch `master` do Extreme não é a comparação principal:** está configurada para Minecraft 1.16.5. A branch `1.21.3` existe, mas não é o mesmo alvo do Rework. O Revived também possui `26.1.2`; sua existência não torna essas APIs diretamente aplicáveis à 1.21.1. A análise detalhada deste dossiê é das branches equivalentes acima. [L01] [V01] [E01]

No início havia 18 arquivos Java modificados e um novo `GuiFrame.java`, relacionados à interface. Foram lidos como parte do estado atual. Nenhum arquivo Java ou recurso do mod foi alterado durante esta análise. As cópias de consulta estão na pasta ignorada `run/audit-references/`.

### 2.2 O que foi feito

- Inspeção de inicialização, configuração, registro, infraestrutura de multibloco, controladores, simulação, portas de itens/energia/fluidos, persistência, menus, periféricos, renderização e integrações.
- Comparação textual por caminho entre Rework e Revived, normalizando os principais nomes de pacotes e o mod id literal.
- Leitura dos sistemas adicionais do Revived e dos principais subsistemas/API do Extreme, incluindo patches históricos específicos.
- Compilação local do Rework e execução de seus GameTests em servidor de desenvolvimento.
- Preparação de recomendações com alvo local, dependências, risco, custo relativo e teste de aceitação.

Não foi feita uma revisão manual exaustiva de cada textura, JSON ou linha dos três projetos. Não foram executados os dois mods de referência, nem comparações de FPS/MSPT, nem testes com todos os mods opcionais. A análise é profunda por subsistema, com limites identificados; não equivale a uma certificação de ausência de bugs.

### 2.3 Vocabulário de evidência

| Classificação | Significado neste documento |
| --- | --- |
| **Executado** | Compilação ou teste realmente executado nesta sessão. |
| **Confirmado no código** | Fluxo, recurso ou defeito diretamente verificável nos arquivos inspecionados. |
| **Indício forte** | O fluxo apresenta um problema específico; falta reproduzir a consequência no jogo. |
| **Hipótese de melhoria** | Há uma solução plausível; benefício depende de medição ou decisão de produto. |

Um commit chamado “fix” em outro projeto não comprova que o Rework tenha o mesmo bug. Cada caso é comparado com o caminho local antes de recomendar algo.

### 2.4 Dimensão do código e parentesco

Contagem de `.java` nos diretórios analisados, incluindo protótipos e código fora do caminho normal de execução:

| Projeto | Arquivos Java | Linhas, incluindo comentários e linhas vazias |
| --- | ---: | ---: |
| Rework | 327 | 36.701 |
| Revived | 208 | 24.375 |
| Extreme | 420 | 47.765 |

Esses números **não medem qualidade**. O Rework incorpora infraestrutura no próprio projeto; o Revived e o Extreme dependem de bibliotecas externas que não entram nessas contagens.

Dos 208 caminhos Java do Revived, 177 têm equivalente no Rework: **98 são textualmente iguais após a normalização limitada**, 79 diferem e 31 existem apenas no Revived por esse critério de caminho. Há 150 caminhos locais sem correspondente no pacote do Revived, em grande parte pelo `core/` incorporado e integrações próprias. Os números não representam equivalência semântica e não descontam protótipos.

Exemplos relevantes de igualdade normalizada: `ModernTurbineSimulation`, a bateria e o tanque dessa simulação, `ModeratorCache` e `SimUtil`. Isso impede atribuir ao Revived uma “turbina melhor” apenas por estar em um fork mais recente.

## 3. Como o Rework funciona e como está organizado

### 3.1 Inicialização e camada comum

`BiggerReactors` é a entrada do mod. Inicializa o registro por anotações, ativa os eventos do núcleo, registra recarga dos dados e carrega ComputerCraft apenas quando presente. A inicialização visual fica em `BiggerReactorsClient`. [L03]

O pacote `core/` concentra o que antes era consumido como biblioteca: registro de blocos/itens/tipos, configuração, leitura de dados, serialização, capabilities, filas, framework de multibloco e componentes de GUI. A autonomia funciona, como demonstrou o servidor de GameTests; em contrapartida, o projeto passa a ser responsável pelas futuras correções desse núcleo. [L04] [L05]

| Diretório | Responsabilidade | Observação para manutenção |
| --- | --- | --- |
| `core/registry` | Descoberta/registro por anotações | Reduz repetição, mas o fluxo é menos evidente que registros explícitos. |
| `core/multiblock` | Associação de peças, montagem, validação, persistência e módulos | Mudanças afetam reator, turbina e trocador simultaneamente. |
| `core/config`, `core/parsers`, `core/data` | TOML/JSON5, regras de configuração e carregamento | Bom candidato a simplificação futura por APIs nativas, sem migração de uma vez. |
| `core/networking`, `core/serialization`, `core/robn` | Transporte e representação dos estados | Exige limites de entrada e versionamento quando o formato evoluir. |
| `multiblocks/reactor` | Reator registrado e sua física | Sistema ativo do mod. |
| `multiblocks/turbine` | Rotor, bobinas, vapor, geração e interface | A física principal coincide com a do Revived normalizado. |
| `multiblocks/heatexchanger` | Dois circuitos térmicos e canais | Sistema adicional importante da linhagem BiggerReactors. |
| `multiblocks/reactor2` | Protótipo herdado | Excluído do Java source set; não é um segundo reator disponível. |
| `machine` | Reprocessador de cianita e configuração por face | Implementação local tem melhorias que devem ser preservadas. |
| `registries` | Moderadores, bobinas e transições líquido/vapor | Dados de gameplay recarregáveis e sincronizados. |
| `compat`, `client/deps` | Integrações opcionais e visualização de receitas | Manter isolamento para servidor sem essas dependências. |
| `gametest` | Testes de conteúdo e montagem | Base útil, cobertura operacional ainda pequena. |

### 3.2 Montagem e ciclo de vida

As peças são blocos e BlockEntities associados a um controlador. Os módulos comuns verificam a estrutura retangular e executam validações em etapas. O objetivo declarado das três etapas é deixar verificações baratas antes das caras. O controlador específico impõe regras internas. [L05] [L06]

No reator, a primeira validação exige terminal e barras de controle e rejeita a mistura de tomadas de energia com portas de refrigerante. Depois verifica colunas completas de combustível, tampas no topo e manifolds conectados. O modo passivo/ativo decorre da presença de portas de refrigerante, e não de dois tipos independentes de núcleo. [L06]

O framework ativo expõe dois estados de montagem: `ASSEMBLED` e `DISASSEMBLED`. Não existe um estado público `PAUSED` nesse enum. A persistência usa um delegado de salvamento e memória do número esperado de peças para controlar reconstituição do conjunto. Por isso, descarregamento de chunks precisa ser testado no fluxo real: não basta transplantar a mudança do Extreme que preserva “ligado”. [L05] [L07]

### 3.3 Caminho de combustível, calor e energia do reator

Fluxo principal:

`item aceito → volume de combustível → irradiação/fertilidade/calor → FE ou vaporização → portas de saída`

A porta de acesso aceita urânio pelas tags comuns, blutônio local e formas em bloco. Os blocos equivalem a nove unidades de lingote. Internamente, `FuelTank` acompanha quantidades de combustível e resíduo e consumo fracionário. A simulação não preserva uma identidade independente para cada combustível inserido. O resíduo é apresentado como cianita; combustível ejetado é apresentado como urânio local. [L08] [L09]

Após validar a estrutura, o controlador monta uma `SimulationDescription`: dimensões internas, propriedades dos moderadores, posições de manifolds e barras. A `SimulationConfiguration` passa parâmetros explicitamente ao motor. Essa separação é uma vantagem de projeto do Rework sobre o uso repetido do singleton de configuração no Revived. Ainda existem dependências do ambiente Minecraft e do `Config` no construtor/seletor; não é correto chamar a simulação inteira de biblioteca completamente independente. [L06] [L10] [L11]

Na simulação passiva, o calor transferido alimenta uma bateria. Na ativa, alimenta um tanque de refrigerante com transição para vapor. Também há troca entre combustível/carcaça, perda para o ambiente e decaimento de fertilidade. O controlador realiza distribuição de energia, ejeção automática de resíduos e movimentação de fluido. [L06] [L13]

**Motor padrão:** `TimeSlicedReactorSimulation`, pois `useFullPassSimulation=false`. Calcula uma amostra de irradiação por tick. Se FullPass for habilitado, o seletor pode usar execução síncrona ou `FullPassReactorSimulation.MultiThreaded`, conforme configuração e quantidade de barras. Os nomes das classes precisam ser tratados com cuidado: `AsyncReactorSimulation.java` existe, mas não é instanciado pelo seletor ativo inspecionado. [L02] [L10] [L14] [L15]

### 3.4 Turbina

O controlador valida mancais, eixo, pás e bobinas; monta configuração do rotor e passa dados das bobinas para `ModernTurbineSimulation`. A turbina usa vapor e produz energia, acompanha RPM, permite engatar/desengatar bobinas e regula vazão e descarte do fluido de saída. [L16] [L17]

O renderer local desenha o rotor por BlockEntityRenderer e avança o ângulo com o tempo real de renderização. O servidor envia atualização do mancal a cada dez ticks. Isso funciona como ponto de partida, mas abre oportunidade para uma política de envio por mudança de RPM como a do Extreme. O Rework não possui a porta dedicada de redstone de turbina que aparece no Extreme. [L16] [L18] [E07]

### 3.5 Trocador de calor

O trocador valida os canais de condensação e evaporação e exige quatro portas, com dois terminais de circuito de cada tipo. Os canais não são apenas decoração: sua contagem e contato entram no modelo térmico. O ciclo transfere calor entre circuitos/corpos térmicos e movimenta fluidos pelas portas. [L19] [L20]

A implementação local dos canais usa modelo incorporado à malha do chunk. O Revived mantém o caminho apoiado em Quartz. A existência de um renderizador com outra biblioteca não demonstra desempenho superior: seria necessário medir colocação/remoção e renderização de estruturas grandes.

### 3.6 Reprocessador

O reprocessador local consome dois lingotes de cianita para produzir um de blutônio, com água e energia. Os valores padrão são 200 ticks, 1 FE/t e 1 mB de água/t, desde que a máquina possa trabalhar; equivalem a 200 FE e 200 mB por ciclo ininterrupto com esses padrões. É uma receita codificada, não um `RecipeType` próprio consultado do mundo. [L12] [L21]

O diferencial local está na infraestrutura: rotas relativas à face da máquina, canais separados de itens/água/energia, controle de entrada/saída e automação. Uma futura receita por datapack deve ser encaixada nessa máquina, preservando esse comportamento.

### 3.7 Dados, recarga e cliente/servidor

Os registros atuais carregam JSON/JSON5 em caminhos próprios (`ebcr/moderators`, `ebest/coils`, `transitions`). Já há uso de tags e sincronização para os clientes. `CoreEvents` avisa a recarga e pede revalidação das estruturas. Portanto, Data Maps seriam uma modernização do mecanismo; **não seriam a primeira vez que o Rework aceitaria dados de modpack**. [L04] [L22] [L23]

`GuiSync` envia estados a cada quatro ticks do servidor. As solicitações só são encaminhadas ao menu aberto e ainda válido. `SimpleChannel` serializa o composto, impõe limite de 1 MiB ao array recebido e encaminha callbacks à thread principal receptora. Esses controles são uma melhoria concreta, mas não validam automaticamente os tipos e valores dentro de cada comando. A porta de redstone é um exemplo de lacuna restante, detalhada adiante. [L24] [L25] [L26]

## 4. O que realmente muda nos projetos de referência

### 4.1 Bigger-Reactors-Revived

O Revived é o parente mais próximo para aproveitamento incremental: mesma versão de Minecraft, mesma família de controladores e grande quantidade de código compartilhado. As diferenças úteis estão principalmente em dados, documentação e ferramentas:

- Provedores de datagen para modelos, estados, loot, receitas, tags, idiomas, geração de mundo e Data Maps.
- Quatro Data Map Types: moderadores de blocos, moderadores de fluidos, bobinas e transições.
- Codecs e registros reconstruídos a partir dos mapas; importador do formato antigo.
- Eventos KubeJS para adicionar e remover entradas por ID/tag.
- Livro Patchouli com modelos multibloco, páginas de receitas e quatro localizações do guia observadas: inglês e três variantes de espanhol.
- Comandos de montagem de reator/turbina/trocador, aliases e histórico de desfazer.
- Opção de escolher o item usado para devolver combustível.
- Correções na seleção da altura de irradiação do TimeSliced e na altura informada quando há um buraco numa coluna de combustível. [V02] [V03] [V04] [V05] [V06] [V07] [V09] [V12]

Custos e limitações observados:

- O build depende dos projetos irmãos Phosphophyllite e Quartz; a configuração aponta para diretórios externos ao repositório principal.
- Patchouli é obrigatório nos metadados da versão analisada. Isso conflita com a atual proposta autônoma do Rework se copiado literalmente.
- A tarefa Gradle `test` está desabilitada. Não foi identificado no diretório principal um conjunto equivalente aos cinco GameTests locais.
- O reprocessador conserva a divergência de chave NBT do tanque.
- Há moderadores adicionais com absorção/eficiência 80, 90 e 200, fora da faixa 0..1 adotada no contrato local. Os codecs do Revived usam `Codec.DOUBLE` sem limitar esses campos. Copiar a tabela inteira pode alterar radicalmente a física.
- As receitas AE2/Mekanism inspecionadas não representam automaticamente funcionalidades ausentes: o Rework já contém receitas condicionais dessas integrações. [V01] [V08] [V10] [V11] [L27]

### 4.2 Extreme Reactors 2

O Extreme possui uma organização diferente: `api/`, configuração, conteúdo/registro e sistemas de multibloco apoiados em ZeroCore. O reator separa controlador, lógica de reação, contêiner de combustível, layout das barras, estatísticas e interfaces de ambiente/leitura/escrita. Essa separação fornece boas referências de contrato, mas não torna a troca de classes direta. [E02] [E03]

Recursos claramente presentes e úteis como referência:

- Combustíveis com propriedades próprias e registros de reagentes, reações e mapeamentos de entrada.
- Variantes Basic/Reinforced com capacidades, limites e eficiências específicas.
- Sensores de turbina para ativação, bobinas, regulagem de fluxo, RPM, energia e fluidos, com comportamentos como limiar e intervalo.
- Porta de carregamento de itens energizados.
- Energizer, reprocessador multibloco e Fluidizer.
- Receitas específicas e serializers para processos.
- Adaptador de capability química do Mekanism, separado do caminho normal de fluido.
- Livro Patchouli com modelos e templates de apresentação.
- `RpmUpdateTracker`, que decide enviar RPM conforme intervalo e diferença.
- Seções do profiler para irradiação, decaimento, reabastecimento, calor e distribuição. [E03] [E04] [E05] [E06] [E07] [E08] [E09] [E10] [E11] [E12]

O modelo de radiação do Extreme não é equivalente ao ray tracing do BiggerReactors. Seu código percorre fontes reais de irradiação e direções, usa propriedades por reagente e combina capacidades térmicas e de saída próprias. Importar suas fórmulas ou coeficientes isoladamente pode quebrar conservação, consumo e progressão.

## 5. Correções e problemas encontrados

As prioridades abaixo são técnicas: **P1** = tratar antes de ampliar a área afetada; **P2** = melhoria relevante com risco controlável; **P3** = opcional/expansão. Não foi demonstrada nesta sessão uma falha que justifique classificar todo o mod como inoperante.

### B01 — Altura incorreta no diagnóstico de coluna de combustível

**Estado:** confirmado no código local; corrigido no Revived. **Prioridade:** P2. **Custo:** pequeno.

Em `validateStage2`, quando falta uma barra na altura `i`, o Rework informa `controlRodPos.getY() + (-1 - i)`. O Revived informa `i`, que é a coordenada de mundo que está sendo consultada. Exemplo: tampa em Y=70, buraco em Y=68; a expressão local informa Y=1, embora o bloco ausente esteja em Y=68. [L06] [V09]

**Adaptação:** transportar apenas a correção da coordenada. **Aceitação:** montar reator em Y positivo e negativo, remover uma barra intermediária e verificar X/Y/Z da mensagem. A validação deve continuar recusando a estrutura; muda a localização do diagnóstico, não a regra.

### B02 — “Reverter” da porta de redstone não executa a reversão

**Estado:** confirmado no código. **Prioridade:** P2. **Custo:** pequeno.

O método `revertChanges()` existe, mas o comando de rede `revertChanges` apenas escreve “No longer implemented!” e deixa a chamada comentada. A tela prepara o botão e seu callback, mas a inclusão do botão na lista de elementos também está comentada. Portanto, o achado é um fluxo de edição desativado/incompleto; não se afirma que um botão visível esteja falhando no jogo atual. O Revived não deve ser considerado fonte de uma correção pronta sem verificar seu caminho equivalente. [L26] [L38]

**Adaptação:** restaurar o contrato do comando e sincronizar o estado de edição com o cliente. **Aceitação:** alterar campos sem aplicar, reverter e verificar que os valores ativos continuam iguais e os campos retornam ao último estado aplicado. Testar dois jogadores editando o mesmo bloco, pois `currentChanges` pertence à tile e é compartilhado.

### B03 — Validação incompleta dos pedidos da porta de redstone

**Estado:** confirmado no código para casts/parsing; consequência em servidor não reproduzida. **Prioridade:** P1. **Custo:** pequeno a médio.

O transporte verifica menu, nome do pedido e tamanho externo, mas a tile faz casts diretos de `Object` para `Integer`, `Boolean` e `String`. `applyChanges` chama `Double.parseDouble` sem tratamento e sem rejeitar `NaN`/infinito. O container encaminha os dados sem uma camada adicional de tipos. Uma tela que filtra caracteres no cliente não protege o receptor contra mensagens inválidas. [L24] [L26] [L28]

**Adaptação:** validar tipo, comprimento e domínio no servidor, interpretar os dois campos em variáveis temporárias e só publicar a alteração se ambos forem válidos. Não deixar metade do estado aplicada quando o segundo campo falhar. O uso de dados tipados nos containers do Extreme é referência de desenho; não foi feita certificação de segurança da implementação de ZeroCore. [E08]

**Aceitação:** enviar campo ausente, tipo errado, texto vazio, texto não numérico, número enorme, `NaN`, infinito e valor válido. Pedido inválido deve ser rejeitado sem exceção escapando do handler e sem modificar a máquina. Esses testes devem usar um jogador com o menu realmente aberto, respeitando o caminho de autorização local.

### B04 — Amostragem vertical enviesada no TimeSliced

**Estado:** cobertura incorreta confirmada pelo contador; correção identificada no Revived; impacto quantitativo na produção não medido. **Prioridade:** P1. **Custo:** médio, incluindo regressão de balanceamento.

O código local incrementa `currentRod`, volta a zero quando alcança o número de colunas, e só depois calcula `yLevel = currentRod % y`. Com **uma coluna e altura interna maior que um**, `yLevel` é sempre zero. Com menos colunas que a altura, parte das alturas nunca é escolhida como origem de irradiação. Isso não significa que nenhum raio alcance as outras alturas; significa que as fontes não amostram todo o volume de maneira uniforme. [L14]

**O Revived tem uma melhoria real aqui:** introduz `activeYLevel`, inicia altura e coluna em -1, avança a altura independentemente e faz rodízio das colunas com offset. A origem dos raios usa esse novo contador. Esse é um dos melhores candidatos de correção a adaptar, preservando a `SimulationConfiguration` local. [V12]

Uma reprodução aritmética dos contadores, sem executar a física, visitou apenas a altura interna 0 no código local com uma coluna e altura 6; o contador do Revived percorre 0 a 5. O inventário de evidências registra outros tamanhos. Isso demonstra a cobertura do contador, não a diferença de FE/t no jogo.

O Extreme itera BlockEntities de combustível agrupadas por altura e escolhe a próxima fonte real. É uma referência de cobertura, não uma fórmula pronta para importar. [E13]

**Adaptação:** usar o rodízio do Revived como referência e verificar cobertura dos pares `(coluna, altura)`, sem multiplicar por acidente a potência média. **Aceitação:** registrar origens amostradas para 1, 2 e várias colunas em alturas diferentes; toda posição prevista deve ser visitada. Depois comparar duas estruturas com moderadores trocados entre camadas e a média de produção/consumo em janela declarada. Definir a tolerância antes de medir.

### B05 — Desligamento no FullPass assíncrono pode repetir consumo antigo

**Estado:** indício forte no fluxo; não reproduzido no jogo. **Prioridade:** P1 para investigação. **Custo:** médio.

`BaseReactorSimulation` marca `wasActiveLastTick=true` quando agenda um ciclo assíncrono e não o redefine para `false`. A condição de coleta continua chamando `radiate()` em ticks desligados. Em `MultiThreaded`, a coleta pode terminar sem novo evento, enquanto `realizeIrradiationTick()` ainda retorna `rawFuelUsage`, calculado no último agendamento. Esse valor é passado a `fuelTank.burn(toBurn)` fora do bloco `active`. O cenário pode continuar consumindo combustível após o desligamento. [L13] [L15]

Não afeta automaticamente o motor padrão: depende de FullPass com caminho assíncrono. Também não se resolve copiando o `tick` do Revived: ele tem outro acoplamento entre coleta e agendamento, com possíveis efeitos próprios ao desligar/religar.

**Adaptação:** explicitar “trabalho pendente” e associar resultado, calor e consumo ao mesmo ciclo; consumir cada resultado uma vez; drenar/cancelar corretamente ao desligar, desmontar ou reconstruir. **Aceitação:** ligar por alguns ticks, desligar e acompanhar combustível, resíduos e resultados pendentes por pelo menos 100 ticks; repetir com zero combustível, barras totalmente inseridas, unload e remontagem. O calor residual pode continuar gerando FE; isso deve ser distinguido de nova queima.

### B06 — Escala de inserção da barra merece revisão física

**Estado:** divergência de escala confirmada; intenção de balanceamento a confirmar. **Prioridade:** P2. **Custo:** médio.

Na emissão usa-se `(100 - insertion) / 100`, mas na interação com uma barra atingida usa-se `insertion * .001` sob comentário “Scale control rod insertion 0..1”. Se a inserção efetiva é 0..100, esse segundo cálculo termina em 0..0,1. O padrão aparece em TimeSliced e FullPass e também no Revived. Não é uma correção já fornecida por ele. [L14] [L15] [V12]

**Adaptação:** confirmar contrato de `ControlRod` e intenção do modelo antes de trocar o fator. **Aceitação:** varrer 0/25/50/75/100% e comparar emissão própria, absorção de radiação alheia, fertilidade e consumo. Mudar essa escala é alteração de balanceamento e precisa de registro de versão.

### B07 — Preservar intenção de funcionamento após desmontagem/unload

**Estado:** diferença de política confirmada; comportamento completo de unload local precisa de reprodução. **Prioridade:** P2. **Custo:** médio.

O Extreme removeu o desligamento forçado no commit `7ccf6f8b`, citando desmontagem, unload e restauração de saves. O reator local chama `setActive(INACTIVE)` em `onDisassembled()`. [C01] [L06]

**Adaptação:** separar “usuário pediu ligado” de “estrutura pode operar agora”. A máquina só processa montada; lembrar a intenção é outra propriedade. Decidir se quebra manual também deve manter a intenção ou se apenas unload deve mantê-la. **Não basta apagar uma linha:** verificar sequência de salvamento, destruição do controlador, merge e restauração do delegado. [L07]

**Aceitação:** teste cruzando dois chunks, unload de um deles, unload completo, reinício do servidor e quebra manual. Confirmar também que redstone de desligamento prevalece ao reabrir o mundo. Na turbina, verificar a semântica de `simulation.reset()` antes de aplicar política semelhante.

### B08 — Robustez numérica e contratos de energia

**Estado:** oportunidade de endurecimento; não foi demonstrada duplicação. **Prioridade:** P2. **Custo:** médio.

O Rework já usa `long` internamente e adapta a capability FE baseada em `int`. Portanto, a adoção de `WideEnergyBuffer` no Extreme não prova que o armazenamento local esteja limitado a 2,1 bilhões. O controlador local distribui com proporção em ponto flutuante, consulta consumidores em simulação/execução e debita o aceite; vale testar consumidores que mudam de capacidade entre chamadas e valores próximos dos limites. [L06] [L16] [L29]

**Adaptação:** especificar invariantes — valor aceito entre zero e o ofertado, débito igual à soma entregue e `simulate` sem mutação — e limitar/conferir respostas na fronteira. Usar os testes para decidir se é necessária mudança do algoritmo. **Aceitação:** várias portas, bateria vazia/cheia, consumidor que deixa de aceitar, remoção de cabo e totais maiores que `Integer.MAX_VALUE`.

### B09 — A lista de correções do Extreme não deve virar uma lista falsa de bugs locais

| Patch do Extreme inspecionado | O que corrige lá | Aplicação ao Rework |
| --- | --- | --- |
| `f99d31f2` | Iteração de `FuelRodsMap` vazio | Não há essa classe no motor local; aproveitar o cenário de teste de estrutura sem fonte. [C02] |
| `12110086` | Capacidade de energia errada no container/painel | O painel local usa outro caminho. Testar capacidade grande, sem importar o patch. [C03] |
| `fa6dfde6` | `getControlRodLocation` retorna mapa X/Y/Z | A API Lua local tem outro contrato; só adicionar alias por compatibilidade deliberada. [C04] |
| `44721b76` | Baldes de fluido | A alteração visível é atualização do ZeroCore. Não há correção equivalente demonstrada nos arquivos locais. [C05] |
| `2a348606` | Crash com versão beta do Sodium | Atinge a tile de rotor do Extreme. Fazer teste de renderização local; não presumir o mesmo crash. [C06] |
| `7ccf6f8b` | Estado ligado preservado | Política aplicável, mas exige adaptação do ciclo de vida local. [C01] |

## 6. Fichas de aproveitamento do Revived

### V-A — Datagen com fonte de verdade clara

**Recomendação:** alta. **Custo:** médio. **Risco:** baixo a médio. **Categoria:** manutenção, sem necessidade de alterar a física.

O Revived possui provedores concretos, não apenas uma tarefa vazia chamada `runData`. O Rework configura execução de dados no Gradle, mas não tem o pacote de provedores equivalente. O ganho é tornar receitas, loot, tags, modelos e traduções repetíveis e verificáveis. [L02] [V02]

**Como portar:** começar com tags e loot; depois receitas e modelos simples. Adaptar todos os IDs para `biggerreactorsrework`, gerar em `src/generated/resources` e escolher a autoridade de cada arquivo. Não manter a mesma saída em `src/main/resources` e em `src/generated/resources` com conteúdos divergentes.

**Cuidados:** o provedor de canais/estados Quartz depende da biblioteca que foi removida; deve ser refeito para os modelos locais. O provedor de compatibilidade usa grandes blocos de JSON como strings: aproveitá-lo como inventário, preferindo builders/objetos tipados onde houver API apropriada. Datagen não precisa carregar todos os mods opcionais em toda execução.

**Aceitação:** duas execuções consecutivas geram os mesmos arquivos; nenhuma saída depende de horário; o build não perde receitas/loot; os GameTests passam; o diff de conteúdo identifica mudanças intencionais. Se o conteúdo atual já está correto, a primeira migração deve preservar os valores.

### V-B — Data Maps nativos para moderadores, bobinas e transições

**Recomendação:** alta, depois de estabelecer baseline dos dados atuais. **Custo:** médio a alto. **Risco:** médio.

Os quatro mapas do Revived usam registro nativo, codecs e sincronização declarada com `.synced(...)`. Isso pode substituir parte do protocolo próprio dos três registros locais e permitir integração mais natural com datapacks NeoForge. O Rework já suporta dados externos; o ganho é reduzir mecanismos particulares e melhorar contrato/validação. [V03] [V04] [L22] [L23]

**Como adaptar:** introduzir modelo de dados imutável validado; implementar leitor nativo e compatibilidade temporária com JSON5; converter a tabela local sem mudar números; publicar snapshot completo dos registros; revalidar simulações existentes após recarga. A publicação deve ocorrer quando o conjunto de dados estiver coerente.

**Não copiar literalmente:** os codecs do Revived não restringem todas as grandezas físicas. Exigir valores finitos, absorção/eficiência no domínio escolhido, moderação e condutividade válidas e calor latente positivo. Documentar explicitamente qualquer exceção de balanceamento. O moderador sentinela interno de FullPass tem absorção negativa por convenção de implementação; ele não deve ser confundido com entrada pública de datapack. [L15] [V04] [V11]

**Precedência observada no Revived:** Data Maps → entradas antigas migradas → adições de script → remoções de script. Isso significa que uma remoção vence uma adição acumulada, mesmo que o autor do script esperasse que a última chamada textual vencesse. O Rework deve escolher e documentar sua política em vez de herdá-la por acidente. [V04]

**Aceitação:** comparação de todos os registros efetivos antes/depois; `/reload` com clientes conectados; novo cliente entrando depois da recarga; tags vazias; mod opcional ausente; duas substituições conflitantes; dado inválido com erro que aponte o arquivo; simulação já montada atualizada sem perder combustível/energia.

### V-C — Integração KubeJS opcional e com sincronização completa

**Recomendação:** alta se modpacks forem público prioritário. **Custo:** médio. **Risco:** médio.

O Revived oferece eventos para moderadores sólidos/fluidos, bobinas e transições, com `add` e `remove`. O guard de carregamento e a classe interna de postagem são boas referências para isolar as dependências. Não é necessário copiar todos os aliases em português/espanhol; uma API pequena e estável pode ser mais fácil de documentar. [V05]

**Como adaptar:** criar contrato público de dados comum a datapacks e scripts; deixar KubeJS apenas como um adaptador opcional. Registrar exemplos de modpack separados do código do mod. Definir se os eventos são de startup ou de recarga: no Revived, a postagem observada parte do common setup.

**Ponto que exige validação:** as listas `scripted*` do Revived são aplicadas depois dos mapas nativos. A sincronização nativa dos Data Maps, por si só, não demonstra que o resultado final dessas listas é transmitido a todo cliente. Não foi demonstrada uma falha de rede no Revived; o contrato não fica provado apenas por haver `.synced`. No Rework, sincronizar o snapshot efetivo final ou integrar as alterações à fonte realmente sincronizada. [V03] [V04]

**Aceitação:** mod inicia sem KubeJS; script adiciona/remove por ID e tag; cliente remoto vê os mesmos valores em tooltip/JEI/EMI que o servidor usa na física; reinício e recarga não duplicam entradas; IDs inválidos produzem mensagem útil.

### V-D — Livro com projeção de estruturas

**Recomendação:** alta. **Custo:** médio para primeira versão, maior para guia completo. **Risco:** baixo para código, médio para conteúdo incorreto.

O Revived já apresenta uma montagem mínima 3×3×3 em página `patchouli:multiblock`, com bloco de combustível, tampa, terminal, acesso e energia. Isso é uma referência prática para transformar o planejamento local em recurso utilizável. O Extreme adiciona templates editoriais e uma organização mais ampla para suas próprias máquinas. [V06] [E12] [L30]

**Como adaptar:** usar o planejamento local como autoridade de escopo; começar com reator passivo, reator ativo e turbina; criar exemplos validados pelo mesmo construtor de teste. Mostrar valores dos dados ativos quando possível. Textos e modelos precisam mencionar portas e regras locais, incluindo o trocador de quatro portas e os manifolds.

**Decisão de dependência:** o Revived requer Patchouli; copiar essa obrigação alteraria a instalação autônoma. Para o Rework, há duas alternativas legítimas: integração opcional com livro quando instalado, ou guia próprio. Definir isso antes de escolher a implementação visual. O arquivo de planejamento não determina sozinho que Patchouli seja obrigatório.

**Aceitação:** receita e acesso ao livro corretos; IDs locais; modelos montam no jogo; PT-BR e inglês inicialmente completos; nenhuma afirmação de FE/t universal sem cenário medido; livro continua legível com receitas modificadas por modpack.

### V-E — Comando de montagem e desfazer para desenvolvimento

**Recomendação:** média, alto valor para QA. **Custo:** médio. **Risco:** alto se copiar literalmente para mundos reais.

O comando do Revived exige permissão nível 2, monta os três tipos e armazena até 16 snapshots por usuário. É útil para reproduzir problemas e montar cenários de teste. [V07]

**Limitações confirmadas na implementação:** o snapshot contém apenas `BlockState`, não NBT de BlockEntities. O comando sobrescreve os blocos e o desfazer restaura estados. Portanto, não recompõe inventário de baú, energia de máquina ou dados de outro bloco substituído. A operação de colocação é síncrona e pode percorrer volumes grandes; o histórico limita quantidade de ações, não orçamento de memória em bytes. O estado estático também precisa de limpeza por ciclo do servidor.

**Como adaptar:** começar em ambiente de desenvolvimento e estruturas pequenas; recusar áreas ocupadas por BlockEntities; limitar volume e altura do mundo; verificar todos os chunks e posições; limpar histórico no encerramento. Para uso administrativo mais amplo, salvar NBT/identidade completos, definir orçamento global e execução por lotes. Um “desfazer” incapaz de restaurar dados não deve ser apresentado como restauração completa.

O seletor de “melhor moderador” do comando também não deve virar recomendação de projeto ideal: uma pontuação escalar de propriedades não substitui simulação da geometria, consumo e objetivo do jogador.

**Aceitação:** tentativas sem permissão recusadas; dimensão/altura válidas; nenhum inventário sobrescrito; cancelamento e desfazer testados; operações grandes não congelam o servidor; estruturas geradas realmente validam.

### V-F — Item de saída de combustível configurável

**Recomendação:** média. **Custo:** pequeno a médio. **Risco:** médio para compatibilidade de modpack.

O Revived usa `FuelOutputItem` para selecionar o item que representa combustível extraído, com fallback para urânio local e validação pela tag de urânio. No Rework, os caminhos inspecionados usam `UraniumIngot.INSTANCE`. [V13] [L08]

**Como adaptar:** aplicar a escolha a `getStackInSlot`, extração manual e ejeção automática de modo consistente. Resolver o ID depois que tags estiverem disponíveis e definir reação a recarga. O construtor do BlockEntity não é um local ideal para congelar uma escolha dependente de tags recarregáveis.

**Limite:** essa opção não cria combustíveis diferentes. Hoje blutônio e urânio convergem para o mesmo volume interno; devolver determinado lingote é uma política de conversão do sistema existente.

**Aceitação:** item configurado ausente, item fora da tag, retorno padrão, extração parcial e todas as rotas de saída. Confirmar que não aparece multiplicação de material na equivalência bloco/lingote.

### V-G — Receitas e materiais de compatibilidade: aproveitar apenas diferenças reais

**Recomendação:** revisão seletiva. **Custo:** pequeno por entrada, maior para balanceamento. **Risco:** médio.

O Rework já tem receitas condicionais de esmagamento/enriquecimento Mekanism e de inscriber AE2. Um arquivo local ainda chamado `grindstone_uranium_ingot.json` já usa `type: ae2:inscriber`; não se deve inferir suporte ausente a partir do nome antigo do arquivo. A receita equivalente do Revived tem o mesmo comportamento essencial, com namespace diferente. [L27] [V10]

O ganho possível está em novos materiais e geração consistente dos arquivos. Comparar valor a valor e conferir a versão instalada de cada integração. Não importar moderadores extremos de Ultimate Foods/Avaritia como se fossem ajustes neutros: absorção/eficiência maiores que 1 alteram as hipóteses do motor. [V11]

**Aceitação:** receita ausente com o mod opcional ausente, presente com ele instalado, proporções preservadas e nenhum ID antigo escapando para o namespace final.

## 7. Fichas de aproveitamento do Extreme Reactors

### E-A — Porta de redstone para turbinas

**Recomendação:** alta. **Custo:** médio. **Risco:** médio.

É uma ampliação funcional concreta: controlar ativação, engate de bobinas e vazão; ler RPM, porcentagem de bateria e quantidade de fluidos. O Extreme oferece comportamentos por nível, pulso, limiar e intervalo. O Rework já dispõe de setters e leituras no controlador/simulação para boa parte desses comandos, mas falta a peça dedicada e sua interface. [E07] [L16] [L17]

**Como adaptar:** construir novo bloco, tile, menu, estado persistido, ícones, receita e tradução usando a infraestrutura local. Reaproveitar a ideia de separar grandeza observada de comportamento do sensor. Começar com comparadores simples e um comando por porta; evitar copiar dependências ZeroCore para isso.

**Escolhas prévias:** unidades, escala analógica 0..15, prioridade entre redstone/computador/tela e histerese para evitar liga/desliga a cada tick. A histerese é recomendação deste dossiê, não um recurso aqui comprovado da referência.

**Aceitação:** pulsos não executados duas vezes; limiares exatos; sinal removido ao desmontar; chunk reload; nenhum loop de atualizações vizinhas; conflitos de comando com comportamento documentado.

### E-B — Sincronização adaptativa de RPM

**Recomendação:** alta após instrumentação. **Custo:** pequeno a médio. **Risco:** baixo a médio.

`RpmUpdateTracker` trata transições zero/não zero imediatamente e combina intervalos mínimo/urgente com diferenças mínima/máxima. O Rework envia atualização de mancais a cada dez ticks, mesmo que a rotação pouco tenha mudado. [E09] [L16]

**Como adaptar:** manter envio inicial e envio ao novo observador; aplicar detector de diferença à velocidade; preservar interpolação do renderer local; nunca usar redução de pacotes para diminuir a taxa da física. Configurar limiares a partir de teste visual.

**Aceitação:** contagem de pacotes e bytes em rotor estável, acelerando e parando; visão correta para jogador entrando no chunk; mesma física; ausência de saltos visíveis. O ganho de tráfego é uma hipótese até medir no cenário local.

### E-C — Interfaces estreitas e separação de responsabilidades

**Recomendação:** média/alta para evolução contínua. **Custo:** médio, distribuído. **Risco:** médio se refatorar tudo de uma vez.

O Extreme distingue leitor, escritor, ambiente e máquina, e concentra cálculos em `ReactorLogic`/`TurbineLogic`. O Rework também já separa a física em interfaces, mas seus controladores agregam validação, reconstrução, coleta, distribuição, render state e pedidos de UI. [E02] [E03] [L06] [L16]

**Como adaptar:** extrair primeiro contratos para telemetria e comandos. GUI, Jade e ComputerCraft podem ler um snapshot coerente em vez de cada integração conhecer estruturas internas. Depois extrair distribuição de energia e políticas de ejeção. Não substituir o framework inteiro nem criar uma classe para cada operação trivial.

**Aceitação:** getters não modificam máquina; comandos validam valores em um ponto; as integrações conservam os contratos públicos; simulação produz os mesmos resultados na configuração de referência.

### E-D — Profiler e diagnóstico técnico

**Recomendação:** alta. **Custo:** pequeno. **Risco:** baixo.

O `ReactorLogic.update()` do Extreme marca fases distintas no profiler. Isso ajuda a diferenciar custo de irradiação, transferência de calor, entrada/saída e distribuição. O Rework tem informação de debug, mas precisa de medição por fase antes de escolher uma otimização grande. [E03] [L06]

**Como adaptar:** instrumentar validação/reconstrução, física, automação, sincronização e desenho do rotor. Manter contagem de peças, tamanho do cache, tempo acumulado e número de tarefas pendentes. Expor diagnóstico técnico por comando de desenvolvimento/log controlado; não despejar detalhes internos no painel normal do jogador.

**Aceitação:** obter perfil de reator pequeno/grande, várias turbinas e trocador, com ambiente, hardware, seed, geometria e configurações registrados. Publicar mediana e percentis de MSPT, além de throughput e consumo. Não concluir “mais rápido” só por reduzir número de classes ou trocar biblioteca.

### E-E — Combustíveis, resíduos e reações extensíveis

**Recomendação:** alta como objetivo de longo prazo; baixa como primeiro port. **Custo:** alto. **Risco:** alto.

O Extreme separa `Reactant`, `FuelProperties`, `Reaction` e mapeamentos de item/fluido. Permite descrever material, propriedades físicas, produto e conversões de entrada. O tanque local registra volumes agregados sem identidade por combustível e usa parâmetros globais de fissão. [E04] [E05] [E06] [L09] [L11]

**Como adaptar:** definir se o núcleo aceita uma única identidade por vez ou mistura; modelar ID, quantidade, fração consumida e produto; introduzir registros validados; atualizar porta de acesso, simulação CPU, NBT, renderização de combustível, GUI, Jade, EMI/JEI e ComputerCraft. Versionar os saves e mapear o combustível antigo para um ID padrão. Não misturar parcialmente o modelo novo com a antiga ejeção fixa de urânio.

**Compatibilidade física:** parâmetros do Extreme não devem ser aplicados às equações locais sem conversão e validação. Mesmo nomes iguais, como moderação e reatividade, não garantem a mesma curva.

**Aceitação:** duas identidades com comportamentos distintos; produto correto; entradas por tag; rejeição de mistura proibida; persistência de fração; upgrade de save; recarga removendo combustível já presente; comparação de massa e energia; nenhum caminho converte um combustível em outro por omissão.

### E-F — Receita de reprocessamento orientada por dados

**Recomendação:** média/alta. **Custo:** médio. **Risco:** médio.

O Extreme possui `ReprocessorRecipe` e serializer dedicado. A arquitetura de receita é aproveitável mesmo sem copiar sua máquina multibloco. No Rework, a primeira etapa pode ser transformar a receita fixa da máquina atual em `RecipeType` próprio. [E10] [L12]

**Como adaptar:** ingredientes e quantidades, fluido, energia e tempo; integração com `RecipeManager`; cache invalidado em reload; JEI e EMI lendo a mesma receita. Definir quando uma receita em andamento é fixada e como reagir à troca de ingredientes. Preservar regras por face e a receita padrão 2:1.

**Aceitação:** duas receitas alternativas por datapack, alteração de receita em reload, saída cheia, falta de água/energia, troca de insumo e reinício no meio do ciclo. Consumo e saída devem corresponder ao mesmo trabalho, sem duplicação.

### E-G — Porta para carregar ferramentas e equipamentos

**Recomendação:** média. **Custo:** médio. **Risco:** baixo a médio.

O Extreme implementa porta de carregamento para itens com energia. É um recurso de utilidade claro que pode ser adicionado sem mudar a física nuclear. [E08]

**Como adaptar:** dois slots, capability de energia do item, limite por tick e débito da bateria do reator/turbina. Respeitar lado lógico e simulação. Decidir se carrega a partir de energia armazenada quando a reação está desligada.

**Aceitação:** item cheio, incompatível, parcialmente carregado, extração durante transferência, falta de energia e retorno após reload; soma de energia entregue nunca maior que o débito.

### E-H — Mekanism químico

**Recomendação:** condicional à demanda de modpack. **Custo:** médio a alto. **Risco:** alto de integração.

O Extreme possui adaptador `IChemicalHandler`, consulta a conversões e serviço opcional de Mekanism. A existência de receitas Mekanism no Rework não significa que suas portas aceitem capabilities químicas: são caminhos diferentes. [E11] [L27]

**Como adaptar:** módulo isolado, carregado apenas com Mekanism; contrato explícito de espécie e razão entre fluido/químico; capacidades e retornos de `simulate`/`execute` corretos. A implementação inspecionada declara `outputFluid` como quantidade aceita, mas retorna diretamente a quantidade do resultado de `insertChemical`; esse retorno precisa ser confrontado com o contrato da versão-alvo do Mekanism antes de qualquer cópia. Não foi reproduzida uma falha desse adaptador nesta sessão.

**Aceitação:** testes com Mekanism presente/ausente, tubos, inventários químicos cheios e parcialmente cheios, conversão ida/volta e consumidores que aceitam só parte. FE, vapor fluido e vapor químico devem ser testados separadamente.

### E-I — Tiers e máquinas adicionais

**Recomendação:** expansão posterior. **Custo:** alto. **Risco:** alto de escopo e balanceamento.

| Sistema do Extreme | Possível valor para o Rework | O que teria de ser criado/adaptado |
| --- | --- | --- |
| Basic/Reinforced | Progressão de custo, tamanho e eficiência | Variantes de blocos/receitas, regras de mistura, limites, saves e material do guia. |
| Energizer | Armazenamento/gestão de energia como multibloco | Controlador, geometria, capacidade, I/O, GUI, periférico e modelo visual. |
| Fluidizer | Cadeia industrial de transformação de materiais/fluidos | Receitas, processo, inventários/tanques, balanço e integrações de receitas. |
| Reprocessador multibloco | Evolução industrial além da máquina atual | Evitar invalidar a máquina básica; definir função e ganho de escala. |
| Novos materiais de bobina | Mais caminhos de progressão | Receitas, tags, propriedades, distribuição de recursos e testes de economia. |

**Não chamar isso de port simples:** as classes estendem o ecossistema ZeroCore. Reutilizar conceitos e partes independentes implica reconstruir a integração na infraestrutura local. Importar ZeroCore só para uma peça reverte uma das decisões centrais do Rework. [E01] [E02] [E14]

## 8. Onde o Rework está melhor e o que preservar

“Melhor” aqui tem um critério declarado: menos dependências obrigatórias, contrato mais claro ou comportamento concreto mais robusto. Não é um ranking global dos autores.

| Ponto local | Evidência | Comparação e ressalva |
| --- | --- | --- |
| Instalação autônoma | Servidor de teste iniciou com apenas Minecraft, NeoForge e Rework | Revived requer Phosphophyllite, Quartz e Patchouli; Extreme requer ZeroCore. Autonomia transfere manutenção ao projeto. |
| Reprocessador persiste água pela mesma chave | `CyaniteReprocessorTile` | Revived ainda lê/grava chaves distintas. O correto é preservar o local e adicionar teste de reload. |
| Reprocessador restringe retirada de energia | `Battery` própria da máquina | A referência usa `EnergyStorage` genérico nos caminhos inspecionados. Preservar a fronteira de entrada local. |
| Configuração por face e automação | `machine/side` e tile local | Não há conjunto equivalente na máquina Revived analisada. Não confundir com o reprocessador multibloco do Extreme. |
| Estado de menu na thread do servidor e transporte serializado | `GuiSync`, `SimpleChannel` | Vantagem verificável local. Não afirmar que os outros têm rede quebrada sem examinar suas bibliotecas. |
| Integrações EMI e Jade | Pacotes locais e changelog | O build/pacote principal do Revived analisado não contém equivalentes; Extreme possui sua própria superfície de integrações. |
| Parâmetros da simulação explícitos | `SimulationConfiguration` | Revived consulta `Config.CONFIG` diretamente em grande parte do motor. O local ainda tem dependências ambientais. |
| Proteção no motor contra resultado inválido | `FuelTank.burn` e checagens de NaN | Útil, porém parcial: não cobre todos os infinitos/domínios nem contratos externos. |
| CI com GameTests | `.github/workflows/ci.yml` | Revived desabilita `test`; verificação de conteúdo local é mais concreta no recorte estudado. Cinco testes ainda não cobrem operação completa. |
| Identidade e suporte do fork claros | `readme.md` e metadados | Revived conserva texto de “official continuation” e issue tracker original; não copiar essa identificação. |
| Renderização sem Quartz | Renderer do rotor e canais locais | Menor dependência obrigatória. Não há evidência nesta sessão de FPS superior. |
| Ferramenta por tag comum | `data/c/tags/item/tools/wrench.json` | Aproveitamento de tag já existe; não importar como novidade a mudança semelhante do Extreme. |

Fontes: [L03] [L12] [L18] [L24] [L25] [L31] [L32] [L33] [V01] [V08] [V14] [E01].

## 9. O que não deve ser portado diretamente

### 9.1 Controladores e framework inteiros

Os nomes podem ser parecidos, mas os contratos de registro, capability, persistência, sincronização e renderização diferem. Copiar o controlador do Revived reintroduziria referências às bibliotecas externas e perderia mudanças de simulação/infraestrutura locais. Copiar o controlador do Extreme exigiria ZeroCore e uma arquitetura de combustível diferente.

**Alternativa:** transportar uma correção pequena ou um comportamento com interface explícita, acompanhado do teste correspondente.

### 9.2 OpenCL e retorno de aceleração por GPU

O Revived conserva código OpenCL e dependências LWJGL. O Rework removeu esse caminho deliberadamente. Presença de código acelerado não comprova que funcione em todas as plataformas, que tenha fallback robusto ou que compense a cópia dos dados. O servidor pode não ter GPU compatível.

**Alternativa:** primeiro medir CPU, corrigir os caminhos avançados e garantir equivalência numérica. Somente depois estudar um backend opcional, sem exigir driver nativo para funcionamento normal. [L02] [L10] [V15]

### 9.3 Protótipo `reactor2`

O código está nas duas linhagens, mas no Rework é explicitamente excluído do build e possui tarefas pendentes. Texturas de barras de cobre/ferro/ouro não demonstram um recurso pronto para ativar. [L02] [L34]

**Alternativa:** tratar como pesquisa de arquitetura própria, com escopo de produto, testes e decisão de manutenção separados. Não recomendar registro automático como melhoria de baixo custo.

### 9.4 Fórmulas e tabelas de balanceamento misturadas

Não copiar constantes do Extreme para a física local por semelhança de nome. Não importar todos os moderadores do Revived sem validação de domínio e objetivo. O ganho de um material depende da geometria, da reação e do conjunto de equações.

**Alternativa:** preservar cenário de referência, variar um parâmetro por vez e registrar resultados. Tratar alteração de potência/consumo como mudança de gameplay.

### 9.5 Compatibilidade de mundos e APIs por acidente

O Rework tem namespace próprio e declara uso em novos mundos. O Revived usa `biggerreactors`; Extreme usa `bigreactors`. Não há migração automática demonstrada entre os três. Manter IDs antigos, mudar mod id ou renomear pacotes não converte NBT, blockstates e APIs de periférico. [L31] [V01] [E01]

**Alternativa:** se futuramente houver demanda, criar um projeto explícito de migração, com cópias de mundos e transformação versionada. A análise atual não recomenda incluir isso no primeiro ciclo.

### 9.6 Supostos novos sistemas de acidente nuclear

O trecho inspecionado do `ReactorLogic` do Extreme contém `TODO: Overload/overheat`. Isso não sustenta recomendar um sistema pronto de meltdown/explosão. Modelar radiação na simulação também não significa automaticamente dano radioativo ao jogador. Não foi estabelecido neste recorte um subsistema completo que possa ser prometido como port pronto. [E03]

### 9.7 Workflows e listas de dependências inteiros

O ambiente de desenvolvimento do Revived carrega muitos mods para compatibilidade e publica builds em fluxos próprios. Isso não é requisito de produção do Rework. A configuração local já separa CI de publicação e possui verificações de versão/branch.

**Alternativa:** manter o pipeline local, adicionar matriz opcional de compatibilidade e preservar testes limpos sem mods adicionais. Não passar a depender de todos os mods usados pelo ambiente de desenvolvimento da referência. [L32] [L33] [V01] [V16]

## 10. Licenças e atribuição observadas nos próprios arquivos

| Projeto | Evidência encontrada | Consequência prática para preparar um port |
| --- | --- | --- |
| Rework | `mod_license=LGPL-2.1-only` e `LICENSE.txt` LGPL 2.1 | Manter licença e créditos de origem do projeto. |
| Revived | `LICENSE`, texto distribuído em recursos e metadados LGPL v2.1 | Registrar arquivos derivados, origem e alterações; preservar avisos aplicáveis. |
| Extreme | `license="MIT"` em `neoforge.mods.toml`; cabeçalhos exigem preservar o aviso | Conservar os cabeçalhos e incluir a atribuição/licença pertinente ao material reutilizado. |

A disponibilidade pública e a declaração de licença permitem estudar e preparar reaproveitamento, mas não significam apagar créditos, avisos ou tratar todo arquivo/dependência como se tivesse a mesma origem. O metadata do GitHub retornou licença não identificada para o Extreme; por isso a evidência usada aqui é o arquivo do próprio projeto. Não foi realizada uma auditoria jurídica de cada asset ou dependência. [L01] [L31] [V17] [E15]

**Procedimento de engenharia recomendado:** para cada futura incorporação, anotar repositório, commit, caminho de origem, escopo copiado/adaptado e avisos preservados. Este dossiê não copiou implementações de gameplay para o mod.

## 11. Matriz consolidada para montar o plano depois

Custos são relativos ao projeto: **pequeno** = correção localizada; **médio** = um subsistema com testes/integração; **alto** = modelo de dados ou gameplay transversal. Não são estimativas em horas. “Primeiro ciclo” é uma recomendação de ordem, não autorização ou implementação já feita.

| ID | Ação | Prioridade | Custo | Risco | Dependência principal | Destino local |
| --- | --- | --- | --- | --- | --- | --- |
| B01 | Corrigir coordenada de erro de barra | P2 | Pequeno | Baixo | Teste de diagnóstico | `ReactorMultiblockController` |
| B02 | Restaurar reverter da porta | P2 | Pequeno | Baixo | Contrato de edição | `ReactorRedstonePortTile` e tela |
| B03 | Validar pedidos e aplicar atomicamente | P1 | Pequeno/médio | Médio | Testes do handler | Portas e containers |
| B04 | Adaptar correção de cobertura vertical do Revived | P1 | Médio | Médio/alto | Baseline numérico | `TimeSlicedReactorSimulation` |
| B05 | Resultado assíncrono consumido uma vez | P1 | Médio | Alto | Teste de estado e ciclo pendente | `BaseReactorSimulation`/FullPass |
| B06 | Rever escala de inserção | P2 | Médio | Alto de balanceamento | Contrato físico | Motores CPU |
| B07 | Preservar intenção após unload | P2 | Médio | Médio/alto | Política de produto e persistência | Controladores + núcleo persistente |
| B08 | Invariantes de energia e adapters | P2 | Médio | Médio | Consumidores de teste | Portas e `EnergyHandlerWrappers` |
| V-A | Datagen por etapas | P2 | Médio | Baixo/médio | Inventário dos recursos | Novo pacote datagen |
| V-B | Data Maps com compatibilidade legada | P2 | Médio/alto | Médio | Contrato e precedência dos dados | `registries`/recarga |
| V-C | KubeJS opcional | P2 | Médio | Médio | Fonte de dados e sync final | Novo adaptador em `compat` |
| V-D | Guia com modelos validados | P2 | Médio | Baixo/médio | Decisão sobre biblioteca de livro | Guia e recursos |
| V-E | Construtor para QA | P2 | Médio | Alto se irrestrito | Política de área e desfazer | Comando de desenvolvimento |
| V-F | Item de combustível extraído configurável | P2 | Pequeno/médio | Médio | Tags e recarga | Porta de acesso/Config |
| V-G | Rever compat e materiais adicionais | P3 | Variável | Médio | Demanda e balanceamento | Dados/receitas |
| E-A | Redstone de turbina | P2 | Médio | Médio | Validação de comando | Novo bloco/tile/menu |
| E-B | RPM enviado por diferença | P2 | Pequeno/médio | Baixo/médio | Medição de tráfego | Controlador e mancal |
| E-C | Contratos de telemetria/comando | P2 | Médio | Médio | Escopo incremental | Controladores e integrações |
| E-D | Instrumentar profiler | P2 | Pequeno | Baixo | Cenários de medição | Tick/validação/renderização |
| E-E | Combustíveis e reações por ID | P3 | Alto | Alto | Modelo e versão de save | Física, portas, UI e integrações |
| E-F | Receita própria de reprocessamento | P2 | Médio | Médio | Política de trabalho em progresso | Máquina e visualizadores |
| E-G | Carregamento de itens | P3 | Médio | Baixo/médio | Contrato da bateria | Nova porta |
| E-H | Adaptador químico Mekanism | P3 | Médio/alto | Alto | Demanda e API exata | `compat` e portas |
| E-I | Tiers/Energizer/Fluidizer | P3 | Alto | Alto | Progressão definida | Novos sistemas |

### 11.1 Ordem de trabalho recomendada

**Etapa 0 — Baseline e correções pequenas.** Congelar cenários de referência; cobrir B01/B02/B03; instrumentar o motor; reproduzir B04/B05/B06 antes de ajustar números. Resultado: menos incerteza para comparar futuras mudanças.

**Etapa 1 — Confiabilidade operacional.** Testes de persistência, chunks, I/O, máquina e simulações avançadas. Resolver a política B07 e os contratos B08. Resultado: novos recursos entram sobre invariantes definidos.

**Etapa 2 — Ferramentas e documentação.** Datagen, exemplos validados, primeira versão do guia e construtor de QA limitado. Pode avançar em grande parte sem alterar a física.

**Etapa 3 — Extensibilidade de dados.** Data Maps, precedência, migração de JSON5 e KubeJS; sincronização testada com cliente remoto. Separar modernização de mecanismo de qualquer mudança de balanceamento.

**Etapa 4 — Recursos pequenos de automação.** Redstone de turbina, sincronização por mudança, combustível ejetado configurável e receitas de reprocessamento. Preservar contratos e dependências opcionais.

**Etapa 5 — Expansão de gameplay.** Só depois decidir combustíveis múltiplos, tiers, química e novas máquinas. Cada um merece documento próprio de design e migração.

## 12. Verificação feita e cobertura que ainda falta

### 12.1 Resultado real desta análise

| Verificação | Resultado | O que demonstra |
| --- | --- | --- |
| Build local, offline | **Passou** | O estado local compila e gera artefato com dependências em cache. `compileJava` estava atualizado; não foi feito clean build. |
| Testes JUnit | **Sem fontes** | O build informou `test NO-SOURCE`; configurar JUnit no Gradle não equivale a ter uma suíte executada. |
| Servidor de GameTests | **5 de 5 passaram** | Inicialização limpa, conteúdo e montagem básica verificadas. |
| Mods opcionais no servidor de teste | **Ausentes** | Não demonstra compatibilidade real com CC, Jade, JEI, EMI, AE2 ou Mekanism. |
| Execução de Extreme/Revived | **Não realizada** | Recursos e correções desses projetos foram avaliados por fonte e histórico. |
| Benchmark comparativo | **Não realizado** | Nenhuma conclusão de FPS/MSPT ou consumo de memória relativo. |

Os cinco testes locais encontrados e executados são `allRecipesLoad`, `compatRecipesLoad`, `blockLootTablesLoad`, `minimalReactorAssembles` e `reactorDisassemblesWhenCasingBroken`. O teste de compatibilidade sem mods adicionais verifica o cenário de ausência; não prova que serializers de mods externos carreguem quando instalados. [L35] [L36]

Os logs indicaram 54 arquivos de moderadores processados para 16 entradas de blocos efetivas, 15 arquivos de bobinas para 4 entradas efetivas e 6 arquivos de transição para 2 entradas de líquido/2 de gás no ambiente limpo. Isso é compatível com entradas baseadas em tags de mods ausentes e não deve ser apresentado automaticamente como perda de dados. Conferir valores efetivos é mais útil que comparar quantidade de arquivos.

### 12.2 Matriz mínima de testes futuros

| Área | Cenário essencial | Invariante/aceitação |
| --- | --- | --- |
| Reator passivo | Combustível, barras, bateria cheia/vazia | Sem energia negativa, conservação de combustível/resíduo e comportamento declarado ao saturar. |
| Reator ativo | Água/vapor e saída bloqueada | Sem fluido criado em `simulate`; pressão de saída não perde volume indevidamente. |
| Simulação | TimeSliced, FullPass síncrono, FullPass assíncrono | Cobertura de amostragem, valores finitos e resultado consumido uma vez; tolerâncias definidas. |
| Geometria | Alturas distintas, coordenadas negativas, fronteira de chunk | Mesmo contrato de validação e diagnóstico correto. |
| Persistência | Salvar no meio da operação e reabrir | Quantidades/fração/estado preservados; nenhum resultado pendente reaplicado. |
| Estrutura | Remover, dividir, unir, unload parcial/completo | Sem duplicar ou perder inventários; identidade do save definida. |
| Turbina | Três eixos, pás/bobinas, vapor sem saída | RPM/energia válidos, comandos corretos e renderização em todos os eixos. |
| Trocador | Circuitos válidos, inválidos e porta bloqueada | Conservação de fluido e comportamento térmico coerente. |
| Reprocessador | Água/energia/saída insuficiente, faces, reload | Sem perda de água no save, sem retirada de energia e sem duplicação de itens. |
| Rede | Valores inválidos, atraso, troca de menu | Mensagem rejeitada, nenhuma exceção não tratada, cliente novo recebe estado correto. |
| Dados | `/reload`, conflitos, remoção e mod ausente | Precedência definida e snapshot igual no servidor/cliente. |
| ComputerCraft | Chamadas durante unload e ticks concorrentes | Acesso ao mundo e mutações no contexto adequado, sem corrida com desmontagem. |
| Visuais | Sodium/Iris, observador entrando no chunk | Sem crash, sem descontinuidade de RPM e sem aumentar dependências obrigatórias. |
| Desempenho | Várias máquinas pequenas e uma grande | MSPT, alocações e tráfego medidos com cenário reproduzível. |

No ComputerCraft local há métodos `@LuaFunction` que chamam controladores diretamente. Isso merece auditoria do contexto de execução de cada método antes de ampliar a API; a presença de alguns `synchronized` não demonstra segurança global de acesso ao mundo. Não foi reproduzida uma corrida nesta sessão. [L37]

## 13. Limites do parecer e decisões pendentes para o plano

O dossiê permite escolher melhorias com base em código real. Antes de transformar tudo em implementação, as decisões que mais afetam escopo são:

1. O mod continuará sem bibliotecas obrigatórias além de NeoForge? Isso determina a estratégia do guia e limita ports dependentes de ZeroCore/Quartz.
2. O objetivo do primeiro ciclo é estabilidade, experiência do jogador ou expansão de progressão? A ordem recomendada prioriza estabilidade.
3. A intenção “ligado” deve sobreviver à quebra manual ou apenas ao unload? B07 depende desse contrato.
4. O balanceamento atual deve permanecer como baseline estrito ou pode mudar para corrigir B04/B06? Correção física pode mudar layouts que hoje rendem mais.
5. Combustíveis múltiplos serão um sistema completo ou haverá apenas customização do item de entrada/saída? São custos muito diferentes.

Essas questões não impediram a análise e não exigem decisão para concluir este documento. Servem de pauta para o plano posterior solicitado.

**Parecer final:** há material valioso nos dois repositórios, sobretudo ferramentas/dados no Revived e automação/contratos/extensibilidade no Extreme. O Rework já tem vantagens concretas que justificam evolução seletiva. O melhor primeiro investimento é fechar as lacunas de confiabilidade e teste, enquanto datagen e guia podem avançar sem substituir a física ou as telas atuais.

## 14. Referências e rastreabilidade

Os códigos **L** apontam para arquivos locais; os códigos **V** e **E** apontam para arquivos das referências fixados nos commits analisados; **C** aponta para patches históricos. As linhas indicam o ponto de entrada do assunto, não toda a extensão de código lida. O inventário de evidências acompanha este dossiê e contém hashes dos arquivos de referência selecionados.

<!-- AUDIT_REFERENCES -->

| Código | Fonte |
| --- | --- |
| [L01] | `gradle.properties`, linha 6 |
| [L02] | `build.gradle`, linha 66 |
| [L03] | `src/main/java/biggerreactorsrework/BiggerReactors.java`, linha 30 |
| [L04] | `src/main/java/biggerreactorsrework/core/CoreEvents.java`, linha 54 |
| [L05] | `src/main/java/biggerreactorsrework/core/multiblock/validated/IValidatedMultiblock.java`, linha 31 |
| [L06] | `src/main/java/biggerreactorsrework/multiblocks/reactor/ReactorMultiblockController.java`, linha 110 |
| [L07] | `src/main/java/biggerreactorsrework/core/multiblock/common/IPersistentMultiblock.java`, linha 50 |
| [L08] | `src/main/java/biggerreactorsrework/multiblocks/reactor/tiles/ReactorAccessPortTile.java`, linha 140 |
| [L09] | `src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/FuelTank.java`, linha 25 |
| [L10] | `src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/SimulationDescription.java`, linha 117 |
| [L11] | `src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/SimulationConfiguration.java`, linha 7 |
| [L12] | `src/main/java/biggerreactorsrework/machine/tiles/CyaniteReprocessorTile.java`, linha 73 |
| [L13] | `src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/BaseReactorSimulation.java`, linha 46 |
| [L14] | `src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/cpu/TimeSlicedReactorSimulation.java`, linha 36 |
| [L15] | `src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/cpu/FullPassReactorSimulation.java`, linha 377 |
| [L16] | `src/main/java/biggerreactorsrework/multiblocks/turbine/TurbineMultiblockController.java`, linha 476 |
| [L17] | `src/main/java/biggerreactorsrework/multiblocks/turbine/simulation/modern/ModernTurbineSimulation.java`, linha 14 |
| [L18] | `src/main/java/biggerreactorsrework/multiblocks/turbine/client/TurbineRotorRenderer.java`, linha 28 |
| [L19] | `src/main/java/biggerreactorsrework/multiblocks/heatexchanger/HeatExchangerMultiblockController.java`, linha 76 |
| [L20] | `src/main/java/biggerreactorsrework/util/FluidTransitionTank.java`, linha 22 |
| [L21] | `src/main/java/biggerreactorsrework/Config.java`, linha 347 |
| [L22] | `src/main/java/biggerreactorsrework/registries/ReactorModeratorRegistry.java`, linha 147 |
| [L23] | `src/main/java/biggerreactorsrework/registries/FluidTransitionRegistry.java`, linha 156 |
| [L24] | `src/main/java/biggerreactorsrework/core/client/gui/GuiSync.java`, linha 113 |
| [L25] | `src/main/java/biggerreactorsrework/core/networking/SimpleChannel.java`, linha 31 |
| [L26] | `src/main/java/biggerreactorsrework/multiblocks/reactor/tiles/ReactorRedstonePortTile.java`, linha 255 |
| [L27] | `src/main/resources/data/biggerreactorsrework/recipe/compat/appliedenergistics/grindstone_uranium_ingot.json`, linha 2 |
| [L28] | `src/main/java/biggerreactorsrework/multiblocks/reactor/containers/ReactorRedstonePortContainer.java`, linha 52 |
| [L29] | `src/main/java/biggerreactorsrework/core/energy/EnergyHandlerWrappers.java`, linha 109 |
| [L30] | `docs/planejamento-guia.md`, linha 1 |
| [L31] | `readme.md`, linha 1 |
| [L32] | `.github/workflows/ci.yml`, linha 1 |
| [L33] | `.github/workflows/release.yml`, linha 1 |
| [L34] | `src/main/java/biggerreactorsrework/multiblocks/reactor2/package-info.java`, linha 1 |
| [L35] | `src/main/java/biggerreactorsrework/gametest/ContentGameTests.java`, linha 22 |
| [L36] | `src/main/java/biggerreactorsrework/gametest/MultiblockGameTests.java`, linha 22 |
| [L37] | `src/main/java/biggerreactorsrework/multiblocks/reactor/deps/ReactorPeripheral.java`, linha 42 |
| [L38] | `src/main/java/biggerreactorsrework/multiblocks/reactor/client/ReactorRedstonePortScreen.java`, linha 224 |
| [V01] | `build.gradle`, linha 181 |
| [V02] | `src/main/java/modernmods/biggerreactorsrevived/datagen/BiggerReactorsDatagen.java`, linha 21 |
| [V03] | `src/main/java/modernmods/biggerreactorsrevived/registries/BiggerReactorsDataMaps.java`, linha 13 |
| [V04] | `src/main/java/modernmods/biggerreactorsrevived/registries/ReactorModeratorRegistry.java`, linha 159 |
| [V05] | `src/main/java/modernmods/biggerreactorsrevived/deps/kubejs/KubeJSDeps.java`, linha 12 |
| [V06] | `src/main/resources/assets/biggerreactors/patchouli_books/guide/en_us/entries/reactors/basic_reactor.json`, linha 12 |
| [V07] | `src/main/java/modernmods/biggerreactorsrevived/commands/MultiblockBuildCommand.java`, linha 430 |
| [V08] | `src/main/java/modernmods/biggerreactorsrevived/machine/tiles/CyaniteReprocessorTile.java`, linha 339 |
| [V09] | `src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/ReactorMultiblockController.java`, linha 153 |
| [V10] | `src/main/java/modernmods/biggerreactorsrevived/datagen/providers/BiggerReactorsCompatRecipeProvider.java`, linha 14 |
| [V11] | `src/main/java/modernmods/biggerreactorsrevived/datagen/providers/BiggerReactorsDataMapProvider.java`, linha 112 |
| [V12] | `src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/simulation/cpu/TimeSlicedReactorSimulation.java`, linha 32 |
| [V13] | `src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/tiles/ReactorAccessPortTile.java`, linha 62 |
| [V14] | `src/main/resources/META-INF/neoforge.mods.toml`, linha 47 |
| [V15] | `src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/simulation/accellerated/ocl/SingleQueueOpenCL12Simulation.java`, linha 19 |
| [V16] | `.github/workflows/build.yml`, linha 1 |
| [V17] | `LICENSE`, linha 1 |
| [E01] | `gradle.properties`, linha 14 |
| [E02] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/MultiblockReactor.java`, linha 78 |
| [E03] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/ReactorLogic.java`, linha 57 |
| [E04] | `src/main/java/it/zerono/mods/extremereactors/api/reactor/FuelProperties.java`, linha 33 |
| [E05] | `src/main/java/it/zerono/mods/extremereactors/api/reactor/ReactantsRegistry.java`, linha 38 |
| [E06] | `src/main/java/it/zerono/mods/extremereactors/api/reactor/ReactionsRegistry.java`, linha 41 |
| [E07] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/turbine/sensor/TurbineSensorType.java`, linha 34 |
| [E08] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/common/container/ChargingPortContainer.java`, linha 41 |
| [E09] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/turbine/RpmUpdateTracker.java`, linha 44 |
| [E10] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reprocessor/recipe/ReprocessorRecipe.java`, linha 33 |
| [E11] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/common/part/fluidport/FluidPortHandlerMekanism.java`, linha 75 |
| [E12] | `src/main/resources/assets/bigreactors/patchouli_books/erguide/en_us/templates/zcmultiblock.json`, linha 1 |
| [E13] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/FuelRodsMap.java`, linha 92 |
| [E14] | `src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/variant/ReactorVariant.java`, linha 33 |
| [E15] | `src/main/resources/META-INF/neoforge.mods.toml`, linha 8 |
| [C01] | `Persistência da intenção de funcionamento` |
| [C02] | `Fonte de irradiação vazia` |
| [C03] | `Capacidade de energia na interface` |
| [C04] | `Localização da barra via computador` |
| [C05] | `Baldes e atualização ZeroCore` |
| [C06] | `Sodium beta e rotor` |

[L01]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/gradle.properties:6>
[L02]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/build.gradle:66>
[L03]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/BiggerReactors.java:30>
[L04]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/core/CoreEvents.java:54>
[L05]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/core/multiblock/validated/IValidatedMultiblock.java:31>
[L06]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/ReactorMultiblockController.java:110>
[L07]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/core/multiblock/common/IPersistentMultiblock.java:50>
[L08]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/tiles/ReactorAccessPortTile.java:140>
[L09]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/FuelTank.java:25>
[L10]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/SimulationDescription.java:117>
[L11]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/SimulationConfiguration.java:7>
[L12]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/machine/tiles/CyaniteReprocessorTile.java:73>
[L13]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/base/BaseReactorSimulation.java:46>
[L14]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/cpu/TimeSlicedReactorSimulation.java:36>
[L15]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/simulation/cpu/FullPassReactorSimulation.java:377>
[L16]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/turbine/TurbineMultiblockController.java:476>
[L17]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/turbine/simulation/modern/ModernTurbineSimulation.java:14>
[L18]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/turbine/client/TurbineRotorRenderer.java:28>
[L19]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/heatexchanger/HeatExchangerMultiblockController.java:76>
[L20]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/util/FluidTransitionTank.java:22>
[L21]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/Config.java:347>
[L22]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/registries/ReactorModeratorRegistry.java:147>
[L23]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/registries/FluidTransitionRegistry.java:156>
[L24]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/core/client/gui/GuiSync.java:113>
[L25]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/core/networking/SimpleChannel.java:31>
[L26]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/tiles/ReactorRedstonePortTile.java:255>
[L27]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/resources/data/biggerreactorsrework/recipe/compat/appliedenergistics/grindstone_uranium_ingot.json:2>
[L28]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/containers/ReactorRedstonePortContainer.java:52>
[L29]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/core/energy/EnergyHandlerWrappers.java:109>
[L30]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/docs/planejamento-guia.md:1>
[L31]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/readme.md:1>
[L32]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/.github/workflows/ci.yml:1>
[L33]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/.github/workflows/release.yml:1>
[L34]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor2/package-info.java:1>
[L35]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/gametest/ContentGameTests.java:22>
[L36]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/gametest/MultiblockGameTests.java:22>
[L37]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/deps/ReactorPeripheral.java:42>
[L38]: <C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/src/main/java/biggerreactorsrework/multiblocks/reactor/client/ReactorRedstonePortScreen.java:224>
[V01]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/build.gradle#L181>
[V02]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/datagen/BiggerReactorsDatagen.java#L21>
[V03]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/registries/BiggerReactorsDataMaps.java#L13>
[V04]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/registries/ReactorModeratorRegistry.java#L159>
[V05]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/deps/kubejs/KubeJSDeps.java#L12>
[V06]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/resources/assets/biggerreactors/patchouli_books/guide/en_us/entries/reactors/basic_reactor.json#L12>
[V07]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/commands/MultiblockBuildCommand.java#L430>
[V08]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/machine/tiles/CyaniteReprocessorTile.java#L339>
[V09]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/ReactorMultiblockController.java#L153>
[V10]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/datagen/providers/BiggerReactorsCompatRecipeProvider.java#L14>
[V11]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/datagen/providers/BiggerReactorsDataMapProvider.java#L112>
[V12]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/simulation/cpu/TimeSlicedReactorSimulation.java#L32>
[V13]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/tiles/ReactorAccessPortTile.java#L62>
[V14]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/resources/META-INF/neoforge.mods.toml#L47>
[V15]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/src/main/java/modernmods/biggerreactorsrevived/multiblocks/reactor/simulation/accellerated/ocl/SingleQueueOpenCL12Simulation.java#L19>
[V16]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/.github/workflows/build.yml#L1>
[V17]: <https://github.com/Modern-Mods/Bigger-Reactors-Revived/blob/d20a6bef5352308638631dbb1bfa6792958ac3b4/LICENSE#L1>
[E01]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/gradle.properties#L14>
[E02]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/MultiblockReactor.java#L78>
[E03]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/ReactorLogic.java#L57>
[E04]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/api/reactor/FuelProperties.java#L33>
[E05]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/api/reactor/ReactantsRegistry.java#L38>
[E06]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/api/reactor/ReactionsRegistry.java#L41>
[E07]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/turbine/sensor/TurbineSensorType.java#L34>
[E08]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/common/container/ChargingPortContainer.java#L41>
[E09]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/turbine/RpmUpdateTracker.java#L44>
[E10]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reprocessor/recipe/ReprocessorRecipe.java#L33>
[E11]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/common/part/fluidport/FluidPortHandlerMekanism.java#L75>
[E12]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/resources/assets/bigreactors/patchouli_books/erguide/en_us/templates/zcmultiblock.json#L1>
[E13]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/FuelRodsMap.java#L92>
[E14]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/java/it/zerono/mods/extremereactors/gamecontent/multiblock/reactor/variant/ReactorVariant.java#L33>
[E15]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/blob/f6cc65140a8abbfc87137f5c54fc641085f66048/src/main/resources/META-INF/neoforge.mods.toml#L8>
[C01]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/commit/7ccf6f8b42cc861e120140a390f7277e2efb45af>
[C02]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/commit/f99d31f27d81c3b1926d55d1f2da124f1fb56cea>
[C03]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/commit/1211008698ffb04c891b59d0acc0b01a4048a43e>
[C04]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/commit/fa6dfde667c652b2032348e10c9718279f2d7c3b>
[C05]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/commit/44721b760eea987bcbad0ffa47f77efaacaed7eb>
[C06]: <https://github.com/ZeroNoRyouki/ExtremeReactors2/commit/2a34860685d7dd96a3167021d8e3d8394fd4ef0e>
