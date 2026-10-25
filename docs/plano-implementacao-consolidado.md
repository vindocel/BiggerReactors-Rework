# Plano de implementação consolidado — BiggerReactors-Rework

**Preparado em 24/10/2026. Estado: planejamento; nenhuma implementação de gameplay iniciada por este documento.**

Este plano transforma a matriz do dossiê em etapas executáveis e incorpora as decisões mais recentes do autor: verificar cada defeito no projeto atual antes de corrigir; estudar o mecanismo de origem antes de adaptar; manter o mod independente; implementar o guia com a tecnologia própria do Dimensional Flux Miner (DFM), seguindo o conteúdo já planejado em `docs/planejamento-guia.md`.

## 1. Regras que valem para todas as etapas

1. **Verificar antes de corrigir.** Um bug descrito no dossiê ou corrigido em outro mod é uma hipótese de trabalho até a revalidação no checkout atual. Se não existir, registrar a evidência, marcar o item como não aplicável ou já resolvido e seguir para o próximo. Não transplantar o patch mesmo assim.
2. **Compreender antes de adaptar.** Ler o caminho completo na origem: entrada, pré-condições, armazenamento, efeitos, persistência, rede e dependências. Uma correção pode depender de uma mudança anterior que não existe aqui.
3. **Adaptar ao Rework.** Usar o controlador, o registro, a serialização, os recursos e os contratos locais. Copiar somente as partes necessárias, substituindo os vínculos com bibliotecas da origem. Renomear imports, sozinho, não elimina dependências.
4. **Sem dependência externa para os recursos do plano.** Núcleo, energia, fluidos, receitas, redstone e guia devem funcionar com Minecraft + NeoForge + Rework. Não adicionar Patchouli, GuideME, ZeroCore, Phosphophyllite, Quartz, o DFM ou uma biblioteca compartilhada obrigatória. Não embutir esses mods, usar submódulo, baixar código em execução ou exigir seus projetos irmãos no build.
5. **Integrações existentes permanecem isoladas e opcionais.** O plano não remove JEI, EMI, Jade ou ComputerCraft já presentes. Nenhum recurso novo pode precisar deles para funcionar. KubeJS e química Mekanism ficam fora do lote principal até haver definição específica; não introduzir APIs externas novas por consequência de adaptar exemplos.
6. **Mesma linha de Minecraft.** Alvo 1.21.1/NeoForge/Java 21. Não atualizar Minecraft, Java, loader ou ferramentas como parte destas adaptações. Usar os sources exatos do projeto para verificar assinaturas.
7. **Preservar o trabalho atual.** Antes de editar cada área, conferir alterações locais, inclusive as telas em andamento. Não reverter trabalho do autor nem substituir componentes locais melhores pelos da referência.
8. **Preservar comportamento fora do item.** Uma modernização de transporte ou de dados não autoriza alterar potência, consumo, vazões, receitas, tamanho ou progressão. Correções físicas precisam ter seus efeitos medidos e documentados.
9. **Servidor é a autoridade.** Validar tipos, domínios, permissões e contexto; simulações de transferência não modificam estado. Cliente não escolhe saldo de energia, inventário ou produção.
10. **Verificar por etapa.** Cada entrega tem critérios de entrada e saída, testes adequados, revisão de logs e registro do que realmente passou. Compilar sozinho não conclui uma mudança de gameplay.
11. **Origem rastreável.** Registrar fonte, revisão, arquivos e adaptação na documentação de engenharia; preservar os avisos aplicáveis. Não importar dependências junto com a implementação nem alterar a licença do Rework.
12. **Sem publicação automática.** Deixar alterações locais para revisão. Este plano não autoriza commit, push, PR, tag ou release.

## 2. Fontes de verdade e referências técnicas

Ordem para executar: instrução mais recente do autor → instruções oficiais do Rework → este plano e planejamento editorial do guia → código/testes atuais como evidência de comportamento → dossiê e fontes externas como referência.

### 2.1 Referências já disponíveis

| Projeto | Linha/revisão consultada | Uso no plano |
| --- | --- | --- |
| Rework | Minecraft 1.21.1; código atual e alterações locais | Destino e primeira verificação de toda tarefa. O HEAD e o diff devem ser registrados novamente no início da execução. |
| ExtremeReactors2 | Branch `1.21`, Minecraft 1.21.1; `f6cc65140a8abbfc87137f5c54fc641085f66048` | Sensores de turbina, rastreador de RPM, receitas e contratos de operação. |
| Bigger-Reactors-Revived | Branch `1.21.1`; `d20a6bef5352308638631dbb1bfa6792958ac3b4` | Correções pequenas, amostragem vertical, datagen e Data Maps. |
| DFM local | Branch identificada em `mc/1.21.1`; propriedades Minecraft 1.21.1 / NeoForge 21.1.255 / Java 21 | Tecnologia do guia próprio. Registrar revisão e alterações locais dos arquivos selecionados quando iniciar a adaptação. |

A análise anterior do Extreme **já utilizou a linha 1.21.1**, não a versão 26. As cópias de consulta existentes em `run/audit-references/` são convenientes, mas ficam em diretório ignorado: uma execução futura deve conferir sua presença e a revisão antes de usá-las. Se o autor fornecer uma cópia diferente, identificar sua versão/revisão e comparar antes de trocar a referência.

### 2.2 Caminhos de consulta

- Dossiê: [dossie-comparativo-reatores-2026-10-24.md](C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/docs/dossie-comparativo-reatores-2026-10-24.md), especialmente seções 5–9 e a matriz da seção 11.
- Evidências com revisões, caminhos e hashes: [dossie-comparativo-evidencias-2026-10-24.json](C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/docs/dossie-comparativo-evidencias-2026-10-24.json).
- Conteúdo obrigatório do guia: [planejamento-guia.md](C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/docs/planejamento-guia.md).
- Instruções oficiais: [AGENTS.md](C:/Users/victo/Documents/Instrucoes/BiggerReactors-Rework/AGENTS.md). Não criar uma cópia divergente dentro do repositório.
- Tecnologia do DFM: `C:/Users/victo/Documents/GitHub/Dimensional-Flux-Miner/src/main/java/dimensionalfluxminer/`, somente leitura.

## 3. Protocolo obrigatório antes de cada correção

### 3.1 Ficha de verificação

Preencher no registro de execução de cada item, antes de alterar a lógica:

| Campo | Registro exigido |
| --- | --- |
| Identificação | ID da matriz, descrição do sintoma e resultado esperado. |
| Estado local | HEAD, arquivos modificados relevantes, versão/configuração e motor realmente usado. |
| Caminho local | Quem chama o código, lado/thread, pré-condições e persistência afetada. Código morto não é caminho ativo. |
| Reprodução | Cenário mínimo, ações, entradas, resultado observado e log/assertiva. |
| Origem | Repositório, revisão e arquivos; mecanismo da correção e dependências envolvidas. |
| Comparação | Causa comum ou apenas sintoma parecido? O Rework já tem outra proteção? |
| Decisão | Corrigir, já resolvido, não aplicável ou inconclusivo, com justificativa. |
| Adaptação | Componentes locais usados, vínculos externos substituídos e comportamento preservado. |
| Aceitação | Teste que detecta o defeito antes e passa depois, mais regressões relevantes. |

### 3.2 Estados e decisão de seguir

| Estado | Quando usar | Próxima ação |
| --- | --- | --- |
| `A_VERIFICAR` | Só existe relato, análise histórica ou hipótese | Inspecionar e reproduzir no checkout atual. |
| `CONFIRMADO` | Causa e caminho ativo demonstrados | Preparar a menor correção adequada e seu teste. |
| `JA_RESOLVIDO` | O sintoma já foi eliminado por mudança local verificável | Não aplicar o patch externo; manter teste útil se houver lacuna real e seguir. |
| `NAO_APLICAVEL` | O caminho, classe, contrato ou pré-condição do bug não existe aqui | Registrar por que o patch não se aplica e seguir. |
| `INCONCLUSIVO` | A hipótese ainda não foi confirmada nem refutada | Não alterar a física por suposição; registrar o cenário faltante e seguir em itens independentes. |
| `IMPLEMENTADO` | Código alterado, validação ainda incompleta | Executar verificações exigidas. |
| `VALIDADO` | Critérios de aceitação passaram | Registrar resultados e atualizar documentação aplicável. |

**Não reproduzir uma vez não basta para afirmar ausência.** Verificar a pré-condição correta, inclusive caminhos opcionais FullPass/assíncronos. Por outro lado, não insistir em adaptar uma correção de uma classe que não participa do motor local.

Para erro aritmético ou diagnóstico determinístico, uma reprodução pequena do contrato pode ser suficiente para estabelecer a causa; mudanças na operação do mundo precisam também de integração. Testes devem verificar comportamento, não repetir a mesma fórmula do código sob teste.

### 3.3 Recursos novos seguem outra regra

Redstone da turbina, receita própria e guia são recursos planejados, não “bugs herdados”. Antes de implementá-los, verificar se já existem no código atual e se atendem ao contrato. Se existirem, aproveitar ou completar apenas a lacuna; se estiverem completos, registrar e pular. **A ausência de um bug não cancela um recurso novo explicitamente solicitado.**

## 4. Escopo e ordem dos lotes

| Lote | Objetivo | Entrada necessária | Saída verificável |
| --- | --- | --- | --- |
| 0 | Revalidar estado e referências | Checkout atual, instruções e cópias de consulta | Inventário, baseline e fichas de triagem. |
| 1 | Correções confirmadas e contratos de estado | Triagem dos B01–B07 | Correções mínimas, sem patches inaplicáveis. |
| 2 | Consolidar energia, fluidos e persistência | Lote 1 nas áreas afetadas | I/O com invariantes testados nos três multiblocos e na máquina. |
| 3 | Receita própria e dados confiáveis | I/O estável, baseline de conteúdo | Reprocessamento por receita, datagen incremental e Data Maps validados. |
| 4 | Automação da turbina e sincronização | Comandos/telemetria seguros | Porta de redstone, sincronização de RPM e documentação. |
| 5 | Guia próprio com tecnologia DFM | APIs/dados consolidados para conteúdo final | Livro completo conforme planejamento editorial, sem biblioteca externa. |
| 6 | Regressão e entrega local | Lotes selecionados implementados | Build, servidor, cliente, testes, JAR auditado e relatório final. |

O estudo e a extração da estrutura de apresentação do guia podem começar antes do lote 5. As tabelas, receitas e instruções finais só devem ser fechadas após os sistemas que documentam estarem estáveis. Não tornar tarefas independentes reféns de uma hipótese inconclusiva.

## 5. Lote 0 — Revalidação e baseline

1. Conferir Minecraft 1.21.1, NeoForge e Java; registrar HEAD/diff e arquivos em edição.
2. Conferir os três motores realmente selecionáveis e os protótipos excluídos. Não ativar `reactor2` ou código morto.
3. Conferir a versão de cada referência, seus arquivos de suporte e chamadas relevantes. Não substituir a comparação por branch mais recente de outra linha.
4. Criar cenários pequenos de reator passivo, ativo, turbina e trocador. Reaproveitar construtores de GameTests quando adequados; registrar geometria, dados, configuração e unidade de cada medição.
5. Repetir build e GameTests existentes na execução de implementação. Os cinco testes aprovados no dossiê são uma observação histórica, não resultado automático de um checkout posterior.
6. Instrumentar fases de validação, reconstrução, física, distribuição e sincronização antes de otimizações grandes; considerar o profiler do Minecraft como referência do Extreme (`ReactorLogic.update`). Não introduzir biblioteca de profiler.

**Saída:** registro de execução com os itens triados e baseline de combustível, resíduos, energia, fluidos, RPM e tempo de processamento nos cenários escolhidos. Valores de desempenho devem incluir condições; não inventar metas universais.

## 6. Lote 1 — Correções condicionais

### 6.1 B01 — Coordenada da barra ausente

- **Origem:** Revived, `multiblocks/reactor/ReactorMultiblockController.java`, `validateStage2`.
- **Como foi feito lá:** usa a altura de mundo `i` que está sendo verificada; não deriva outra altura a partir da tampa.
- **Verificação local:** reator em altura positiva e negativa, coluna com falha intermediária; comparar a mensagem com o bloco efetivamente ausente.
- **Adaptação:** se persistir a expressão incorreta local, trocar apenas a coordenada do diagnóstico. Não substituir o controlador.
- **Aceitação:** X/Y/Z corretos, mesma rejeição estrutural e nenhuma alteração de simulação.

### 6.2 B02/B03 — Edição e validação da porta de redstone do reator

- **Origem:** os problemas foram identificados no código local; não há promessa de patch pronto no Revived. Os dados tipados dos containers do Extreme servem como referência de contrato.
- **Verificação local:** conferir se reverter ainda está desativado e se o botão está realmente registrado na tela. Testar handlers com tipos inválidos, texto não numérico e valores não finitos, com menu válido aberto.
- **Adaptação:** validar tipo e domínio no servidor; interpretar todos os campos antes de publicar; aplicar atomicamente. Se o fluxo de reversão fizer parte da interface a concluir, restaurar comando e botão de forma coerente.
- **Cuidado:** o estado de edição hoje pertence à tile; testar dois jogadores. Não usar a correção como pretexto para redesenhar as telas em andamento.
- **Aceitação:** pedidos inválidos não escapam como exceção, não alteram parcialmente o estado e não alcançam outra máquina. Aplicar/reverter tem comportamento consistente e sincronizado.

### 6.3 B04 — Seleção vertical de irradiação

- **Origem:** Revived, `simulation/cpu/TimeSlicedReactorSimulation.java`.
- **Como foi feito lá:** `activeYLevel` independente da coluna, inicialização em -1, avanço modular da altura e rodízio com offset entre colunas. Os raios partem dessa altura.
- **Verificação local:** confirmar o código atual; rastrear as origens de amostragem com uma, duas e várias colunas em alturas distintas. A reprodução aritmética do dossiê não substitui a medição da física.
- **Adaptação:** transferir o mecanismo de rodízio, preservando `SimulationConfiguration` e as equações locais. Não copiar o acesso global ao `Config` usado na origem.
- **Aceitação:** cobertura dos pares coluna/altura em janela suficiente; produção/consumo comparados sob cenário e tolerância definidos; efeito físico documentado. Se o contador já foi corrigido, pular a implementação.

### 6.4 B05 — Resultado assíncrono e consumo após desligar

- **Origem:** hipótese específica do Rework em `BaseReactorSimulation`/FullPass; o `tick` do Revived não deve ser copiado, pois seu agendamento é diferente.
- **Verificação local:** habilitar FullPass assíncrono, ligar/desligar, acompanhar combustível e eventos por vários ticks; repetir com combustível esgotado e tarefas pendentes. Calor residual gerando FE não prova nova queima.
- **Adaptação, se confirmado:** associar resultado térmico e combustível ao mesmo trabalho; coletar cada trabalho uma vez; encerrar/drenar corretamente ao desligar, desmontar e reconstruir. Nenhum acesso ao mundo na thread de cálculo.
- **Aceitação:** sem repetição de consumo ou reaplicação de resultados, nem corrida em unload; caminhos padrão/síncrono preservados.

### 6.5 B06 — Escala de inserção

- **Origem:** divergência observada nos motores locais e no Revived; não é correção fornecida por ele.
- **Verificação local:** confirmar domínio real da inserção e intenção da equação; medir 0/25/50/75/100% e interação com barras atingidas por radiação.
- **Adaptação:** corrigir somente se demonstrada incompatibilidade entre domínio e fórmula. Não trocar `.001` por outro fator apenas pelo comentário.
- **Aceitação:** curvas de emissão, absorção, fertilidade e consumo coerentes. Se exigir escolher novo balanceamento, registrar a decisão necessária e continuar as tarefas independentes.

### 6.6 B07 — Intenção de ligado e ciclo de vida

- **Origem:** Extreme, patch `7ccf6f8b`, que remove desligamento forçado ao desmontar.
- **Verificação local:** unload parcial/completo, reinício, quebra manual e remontagem; conferir delegado de persistência e estado restaurado.
- **Adaptação:** separar intenção do usuário de capacidade atual de operar. A estrutura desmontada não processa. Não apagar simplesmente `setActive(INACTIVE)`.
- **Decisão pendente para este item:** se quebra manual deve desligar ou preservar intenção. O autor aprovou o planejamento, mas não definiu essa política. Não pedir decisão para estudar/reproduzir; obter definição antes de mudar esse comportamento.
- **Aceitação:** estado consistente após reload, redstone prevalecendo conforme contrato, e nenhum processamento sem estrutura válida. Verificar também o reset da turbina.

## 7. Lote 2 — Energia, fluidos e persistência

### 7.1 Energia — B08, E-C e E-D

**Referências:** armazenamento e distribuição do Extreme, suas interfaces de leitura/escrita e marcações de profiler. O Rework já usa `long` internamente e tem `EnergyHandlerWrappers`; não substituir por ZeroCore ou introduzir seu `WideAmount`.

**Trabalho planejado:**

1. Inventariar bateria do reator, bateria da turbina, consumo do reprocessador, tomadas e adaptadores FE.
2. Definir um contrato local de transferência com saldo/capacidade/taxa separados e leituras sem efeito colateral.
3. Testar o algoritmo atual antes de unificá-lo; consolidar validação e lógica comum comprovadamente repetida. Manter políticas específicas de gerar/exportar e receber/consumir.
4. Validar resposta externa: aceite nunca negativo nem maior que a oferta. Debitar somente o efetivamente entregue, sem segunda extração indevida.
5. Tratar soma, overflow, divisão por zero, arredondamento e fronteira `long`/`int`. Limitar a fronteira FE sem truncar telemetria interna ou da GUI.
6. Testar invalidação de capabilities, remoção de cabo, consumidor que muda entre consulta/execução e troca de chunks.
7. Expor telemetria coerente para telas, Jade e CC existentes por interfaces locais, sem duplicar cálculos.

**Invariantes:** saldo não negativo e dentro da política de capacidade; simulação não muda estado; energia debitada corresponde à entregue/consumida; retirar energia do reprocessador continua proibido. Não alterar capacidade ou limite por tick para imitar o Extreme.

**Aceitação:** testes de fronteira, várias portas concorrendo pela bateria, saldos superiores a `Integer.MAX_VALUE`, mundo salvo/reaberto e nenhuma alteração de produção em cenários equivalentes. Se uma suspeita não se confirmar, conservar o algoritmo válido e documentar a conclusão.

### 7.2 Fluidos — consolidação adicional explicitada pelo autor

**Referências:** separação de portas/handlers no Extreme e contratos locais de `IFluidTankHandler`, wrappers, `FluidTransitionTank`, `ReactorTransitionTank` e tanques da turbina. Esta tarefa amplia B08 para fluidos; a adoção de Data Maps é outra etapa.

**Trabalho planejado:**

1. Mapear água/vapor do reator, vapor/condensado da turbina, os dois circuitos do trocador e água do reprocessador.
2. Conferir unidade e intervalo das capacidades internas e dos adapters NeoForge; preservar espécie de fluido e componentes quando aplicáveis.
3. Consolidar verificações de quantidade, compatibilidade, lado/direção e simulação. Não confundir capacidade em mB com vazão em mB/t.
4. Descrever cada transição: entrada, saída, razão de conversão, calor e condições de aceitação. Conservação deve respeitar a transformação configurada; não exigir igualdade cega entre todos os volumes quando o modelo definir outra razão.
5. Garantir que saída cheia não destrua entrada nem gere trabalho sem destino de forma diferente do contrato. Não modificar uma política de descarte já existente sem justificativa.
6. Conferir troca de transição ativa, esvaziamento parcial, dump intencional, mudança de direção e reload de dados.
7. Tratar invalidação dos vizinhos e volumes persistidos em formação, desmontagem, união/divisão e unload.

**Aceitação:** `simulate` sem mutação; transferência parcial correta; fluidos incompatíveis recusados; nenhum volume negativo ou criado por adapter; tanques preservados em reload; quatro portas do trocador verificadas. Testar o ciclo reator → turbina → retorno em mundo de teste.

Não implementar química Mekanism nesta consolidação. Fluido NeoForge e químico Mekanism são contratos diferentes; suporte ao primeiro não promete o segundo.

### 7.3 Persistência e recuperação

- Registrar o formato atual antes de alterar modelos; conservar os saves existentes do próprio Rework nos caminhos não alterados.
- Não criar migração de mundos do BiggerReactors original ou do Extreme.
- Não usar união de NBTs ou soma de saldos sem provar que não se trata da mesma cópia de estado.
- Testar combustível fracionário, resíduos, bateria, tanques e trabalho em andamento.
- Qualquer evolução de formato necessária deve ter versão e política explícita; não descartar silenciosamente quantidades antigas.

## 8. Lote 3 — Receitas e dados

### 8.1 E-F — Receita própria do reprocessador

**Origem:** Extreme, `gamecontent/multiblock/reprocessor/recipe/ReprocessorRecipe.java` e fluxo de processo da sua máquina. Aproveitar a ideia de receita serializada, não seu reprocessador multibloco ou seus ingredientes específicos de ZeroCore.

**Destino:** máquina atual, incluindo configuração de faces, inventários, água, bateria, tela e integração com visualizadores existentes.

**Passos:**

1. Conferir novamente se a máquina continua com processo fixo. Inventariar contagens, duração, consumo por tick, interrupção e salvamento.
2. Definir `RecipeType` e serializer locais usando as APIs Minecraft/NeoForge 1.21.1, com codec persistente e de rede adequados à versão. Não adicionar biblioteca de receitas.
3. Modelar ingredientes/quantidades, saída, fluido e custo/duração com validação de domínio e valores finitos. Definir a unidade dos custos no contrato; não misturar custo total e custo por tick.
4. **Preservar os padrões atuais e as opções de configuração.** A receita padrão continua 2 cianitas → 1 blutônio, com os custos/duração atuais. Para configurações já alteradas pelo jogador, definir uma fonte de verdade: receita com custos opcionais resolvidos pela configuração atual, ou outra compatibilidade explícita. Não criar defaults JSON independentes que neutralizem o TOML sem aviso.
5. Ler a receita pelo `RecipeManager`; definir escolha determinística quando mais de uma corresponder. Cache acompanha identidade/recarga do gerenciador.
6. Fixar a identidade/condições do trabalho em andamento ou definir invalidação segura. Não trocar receita no fim usando consumo realizado para outra.
7. Atualizar JEI/EMI existentes para ler a mesma receita efetiva da máquina; o núcleo continua operando sem visualizador.
8. Persistir progresso e identidade necessários; tratar receita removida no reload, saída cheia, troca de insumo e reinício.

**Aceitação:** receita padrão mantém resultado e custos efetivos; receita alternativa por datapack funciona; transferência por face preservada; nenhum produto sem consumo; nenhum consumo duplicado; cliente remoto e servidor concordam. Adicionar representação nativa ao guia para receita de máquina, pois o elemento de crafting do DFM não cobre automaticamente este processo.

### 8.2 V-A — Datagen incremental

**Origem:** `datagen/BiggerReactorsDatagen.java` e provedores do Revived.

**Como adaptar:** começar por loot/tags; seguir para receitas/modelos simples e idiomas. Preservar IDs/valores, definir qual pasta contém a saída e remover duplicação só depois de comparar o resultado gerado. Adaptar os canais aos modelos próprios; não transportar provedores Quartz.

**Aceitação:** execução repetível, diff intencional, nenhum mod opcional exigido para datagen e todos os recursos carregando em ambiente limpo. A nova receita e a nova porta devem ter todos os assets, tags, loot e traduções correspondentes.

### 8.3 V-B — Data Maps e validação de dados

**Origem:** Revived, `BiggerReactorsDataMaps`, registros, `ScriptedEntry` e `LegacyRegistryMigration`.

**Como foi feito lá:** quatro mapas nativos, codecs, reconstrução dos registros e importação das entradas antigas. A implementação também aplica listas de scripts depois dos mapas.

**Adaptação local:**

- Quatro tipos locais para moderadores de bloco/fluido, bobinas e transições; APIs nativas já fornecidas pelo loader.
- Converter tabela atual sem rebalancear materiais. Validar os domínios e erros por arquivo, não copiar codecs permissivos ou valores extremos por conveniência.
- Definir precedência entre conteúdo base, formato legado e Data Maps. Manter leitura compatível do JSON5 durante a transição; não transformar essa modernização em migração de mundos originais.
- Publicar um conjunto coerente e revalidar máquinas existentes sem perder estado; invalidar caches quando dados/configuração mudarem.
- Sincronizar o resultado efetivo, incluindo quaisquer camadas futuras. Evitar dois protocolos que alimentem o cliente com versões diferentes do mesmo registro.

**Aceitação:** registros efetivos equivalentes no cenário inicial; reload sem crash; cliente conectado e cliente novo recebem o mesmo conjunto; tags/mods ausentes não produzem entradas inválidas; dados malformados são rejeitados com diagnóstico útil.

### 8.4 V-F/V-G — Combustível extraído e compatibilidade existente

**Item de saída:** o Revived resolve um item configurável e o usa no slot/extração/ejeção. Verificar se há necessidade real local; se incluído, resolver ID/tag no momento correto e manter as três saídas consistentes após reload. Isso não implementa combustíveis com física distinta. ID inválido precisa de fallback documentado.

**Receitas/materiais:** comparar arquivos e registros efetivos antes de importar algo. O Rework já tem compat AE2/Mekanism; nomes de arquivos antigos não provam ausência de funcionalidade. Alterar somente diferenças úteis, com ausência do mod opcional validada. Novos materiais e números de balanceamento precisam de definição específica.

## 9. Lote 4 — Redstone da turbina e sincronização

### 9.1 E-A — Porta de redstone da turbina

**Origem:** Extreme, `TurbineSensorType`, `TurbineSensorSetting`, `TurbineRedstonePortEntity` e container. Ler os quatro antes de implementar; classes base ZeroCore devem ser substituídas por mecanismos locais.

**Contrato funcional planejado:**

- Comandos: ativar/desativar turbina, engatar/desengatar bobinas e ajustar vazão dentro dos limites atuais.
- Sensores: RPM, energia armazenada/percentual e quantidades de admissão/saída.
- Comportamentos de referência: nível/pulso para comandos e limiares/intervalo para sensores, com tipos/unidades explícitos.

**Adaptação:**

1. Revalidar ausência da porta e inventariar setters seguros no controlador/simulação.
2. Criar bloco, tile, container, estado, receita, modelos, loot, tags, traduções e tela usando o padrão local. A porta é peça de face externa; as regras de posicionamento devem ser verificadas pelo validador.
3. Separar grandeza medida, comportamento do sensor e parâmetros. Reusar validação do lote 1 em comandos do servidor; não reutilizar strings soltas como números válidos.
4. Fazer comandos na thread correta e após validar controlador/estrutura; desligar saída de sinal quando a estrutura não puder fornecer o dado.
5. Documentar prioridade entre tela, redstone e ComputerCraft. Não mudar a semântica das APIs existentes sem compatibilidade.
6. Definir histerese/debounce apenas onde houver comportamento aprovado; não adicionar temporização que altere pulsos por acidente.
7. Atualizar testes de contagem de conteúdo e o guia quando os novos recursos estiverem registrados.

**Decisões a fechar antes do item correspondente:** receita/custo da peça, escala analógica se implementada, semântica dos intervalos e conflito entre comandos persistentes. Descrever proposta concreta na tarefa de implementação; não inventar balanceamento neste plano. É possível implementar/testar a base da porta enquanto essas partes independentes são especificadas.

**Aceitação:** pulso único, sinal contínuo, fronteiras dos limiares, unload/reload, três orientações do rotor, estrutura desmontada e dois emissores. Nenhuma oscilação ou cascata de atualizações de vizinhos sem necessidade. O recurso deve funcionar sem ComputerCraft ou outro mod de automação.

### 9.2 E-B — Sincronização de RPM por mudança

**Origem:** Extreme, `RpmUpdateTracker` e seu uso em `MultiblockTurbine`.

**Como foi feito lá:** intervalos mínimo/urgente e diferenças mínima/máxima; transição zero/não zero recebe tratamento imediato.

**Adaptação:** medir os envios locais antes; manter sincronização inicial, de novo observador e de mudança de estrutura; reduzir somente envios redundantes da velocidade. Conservar interpolação do renderer e frequência da física. Os limiares devem vir da medição local, não de promessa de ganho.

**Aceitação:** menos envios no cenário estável, parada/partida corretas, jogador entrando no chunk com velocidade atual e ausência de saltos visíveis. Se a política atual já atender e a mudança não trouxer benefício, registrar e não aumentar a complexidade.

## 10. Lote 5 — Guia próprio com tecnologia do DFM

### 10.1 Decisão fechada

**Tecnologia:** código próprio adaptado do guia local do DFM, linha 1.21.1.  
**Conteúdo:** conforme a revisão do autor de 24/10/2026, cinco capítulos e os critérios de [planejamento-guia.md](C:/Users/victo/Documents/GitHub/BiggerReactors-Rework/docs/planejamento-guia.md). As 14 áreas editoriais anteriores ficam agrupadas por máquina. Limites mínimos/máximos são textuais; não exibir modelo 3D máximo.  
**Referências complementares:** ideias editoriais, exemplos e formas de explicar das referências Extreme/Revived.  
**Dependências:** nenhuma biblioteca de guia, nem DFM instalado.

Esta decisão substitui as alternativas Patchouli/guia próprio ainda abertas no dossiê. Não há nova decisão de biblioteca a pedir ao autor.

### 10.2 Mapa da tecnologia de origem e adaptação

Os caminhos abaixo são relativos a `C:/Users/victo/Documents/GitHub/Dimensional-Flux-Miner/src/main/java/dimensionalfluxminer/`. Os nomes de destino são uma organização proposta, a conferir com o código existente.

| Fonte DFM inspecionada | O que faz lá | Adaptação planejada no Rework |
| --- | --- | --- |
| `content/GuideBookItem.java` | Uso do item abre o guia no cliente | Item/registro/receita locais e callback cliente; comum sem importar classes de tela. |
| `machine/ClientHooks.java` e inicialização cliente | Callback evita vínculo de classe cliente no código comum | Reusar o isolamento cliente já existente no Rework; callback mínimo para o guia, sem copiar hooks de som/mineração. |
| `client/screen/GuideScreen.java` | Tela, abertura, páginas, navegação, cliques e tooltips | Tela própria com namespace e textos locais; manter comportamento de navegação e adequar recursos à identidade visual autorizada. |
| `client/screen/GuideBook.java` | Layout/paginação de elementos e índice | Paginação própria para os capítulos do plano editorial; recalcular quando dados/idioma/resolução mudarem. |
| `client/screen/GuideElement.java` | Contrato de medição, desenho, clique e arraste | Contrato local pequeno para elementos do livro. |
| `client/screen/GuideElements.java` | Texto, notas, itens, crafting e tabelas | Elementos equivalentes usando registros locais; adicionar elemento de reprocessamento, sem tratá-lo como crafting 3×3. |
| `client/screen/GuideContent.java` | Builder de capítulos com dados e receitas reais | Reescrever conteúdo a partir de `planejamento-guia.md`; substituir fórmulas/configs/itens DFM por dados atuais do Rework. |
| `client/screen/GuideReferences.java` | Referências editoriais com tooltip de item/fluido e aliases | Política de referências dos capítulos locais, com traduções PT-BR/inglês; remover toda a tabela de itens da mineradora. |
| `client/screen/GuideMultiblock.java` | Visão 3D girável, camadas e destaque por peça | Generalizar para definições de exemplos de reator/turbina/trocador; estado por exemplo e quantidade variável de camadas. |
| `client/render/GuideBookAnimation.java`, `GuideBookRenderer.java` | Animação/renderização do item-livro | Adaptar o necessário para o mesmo funcionamento, com modelos e recursos locais; substituir utilitários exclusivos do DFM. |

### 10.3 Pontos que impedem cópia literal

1. **Conteúdo acoplado:** `GuideContent` importa itens, registros e fórmulas do DFM. Transportar apenas builders/layout; o texto e os dados vêm do Rework.
2. **Modelo 3D específico:** `GuideMultiblock` usa `MinerLayout`, `MinerStructure`, um modelo formado específico e seleção de três camadas. O Rework precisa de vários modelos de tamanhos diferentes. Extrair rotação/corte/highlight e substituir a fonte geométrica; não importar o multibloco da mineradora.
3. **Estado compartilhado:** rotação/camada/destaque usam estado estático na referência. Evitar que trocar entre exemplos reutilize camada inválida ou destaque de outra máquina.
4. **Receitas:** o elemento DFM de crafting consulta o `RecipeManager` e mostra receita ausente de forma legível; preservar essa ideia. Criar representação específica do reprocessamento com ingredientes, fluido, tempo e energia efetivos.
5. **Dados no cliente:** as tabelas do guia precisam refletir os registros/configs sincronizados. Não ler singleton servidor de outro mundo ou manter cache permanente entre servidores.
6. **Visualizadores opcionais:** botões DFM importam `RecipeViewers`. Substituir pelo bridge local de JEI/EMI já existente ou ocultar o botão sem viewer. O guia permanece completo e utilizável sem viewer.
7. **Recursos e atribuição:** adaptar namespace, modelos, texturas e traduções; conferir procedência dos arquivos selecionados e avisos. O autor autorizou o aproveitamento da tecnologia do seu guia; isso não autoriza retirar avisos de terceiros eventualmente presentes.

### 10.4 Sequência de implementação do guia

**G1 — Base técnica.** Adaptar item, callback cliente, tela, paginação e elementos. Validar abertura/fechamento, navegação, redimensionamento e servidor dedicado sem classes cliente. Incluir a animação/modelos do livro necessários ao funcionamento selecionado.

**G2 — Dados e navegação.** Criar os capítulos conforme o plano editorial; separar tradução de definição estrutural; configurar referências de itens/fluidos. Receitas e tabelas devem usar dados reais do mundo, com fallback claro para conteúdo ausente.

**G3 — Exemplos multibloco.** Definir modelos locais para reator mínimo, reator com moderadores, reator ativo, turbina e trocador. Gerar lista de peças a partir da mesma definição do exemplo; validar a montagem em GameTest/mundo de teste. Incluir rotação, corte por camada e destaque da peça.

**G4 — Conteúdo completo.** Organizar em cinco capítulos: Primeiros passos, Materiais, Reator, Turbina e Trocador de calor. Incorporar peças, montagem, operação, moderação, automação e diagnóstico dentro do capítulo correspondente. Reator passivo e ativo pertencem ao mesmo capítulo. Mostrar fabricação e operação do reprocessador junto da obtenção do blutônio em Materiais, sem entrada separada no índice. Informar limites configurados em texto e manter somente exemplos pequenos em 3D.

**G5 — Validação editorial e visual.** Conferir PT-BR/inglês, unidades, páginas sem sobreposição, tabelas longas, tooltips, receitas alteradas por datapack e modelos em diversas escalas. Medir os exemplos de rendimento que forem publicados; não importar FE/t, RPM ideal ou custo histórico sem conferência.

**Aceitação do guia:** abre com somente Rework/NeoForge; nenhum import/ID/recurso de DFM/guia externo necessário em execução; capítulos completos conforme o plano; modelos montam de verdade; receitas e tabelas acompanham reload/servidor; cliente e servidor dedicado iniciam sem erros.

Projeção de uma estrutura **no mundo** não foi estabelecida como parte da tecnologia DFM inspecionada: ela mostra modelo 3D na página. Não prometer projeção de mundo por copiar uma ideia do livro Patchouli; avaliar separadamente se for solicitada depois.

## 11. Destino de todos os itens da matriz original

| ID original | Destino neste plano | Regra de execução |
| --- | --- | --- |
| B01 | Lote 1, diagnóstico | Corrigir somente se reproduzido no código atual. |
| B02 | Lote 1, edição/reverter | Conferir fluxo ativo; concluir apenas a lacuna existente. |
| B03 | Lote 1, validação | Testar entrada no servidor antes da alteração. |
| B04 | Lote 1, rodízio vertical | Adaptar mecanismo Revived se o defeito persistir. |
| B05 | Lote 1, resultado assíncrono | Reproduzir no motor correto; não copiar `tick` incompatível. |
| B06 | Lote 1, escala de inserção | Sem troca de fator por suposição; fechar contrato físico. |
| B07 | Lote 1/2, intenção/persistência | Reproduzir unload; definir política de quebra manual antes de mudar. |
| B08 | Lote 2, energia; complementado por fluidos | Testar contratos e consolidar sem alterar produção. |
| V-A | Lote 3, datagen | Provedores nativos, sem Quartz e sem dados duplicados. |
| V-B | Lote 3, Data Maps | Usar APIs do NeoForge já fornecidas e validar a migração dos dados. |
| V-C | Fora do lote principal: KubeJS | Não incluir dependência nova neste plano de núcleo/guia; definição específica posterior. |
| V-D | Lote 5, guia DFM | Tecnologia já definida; conteúdo segue `planejamento-guia.md`. |
| V-E | Ferramenta de QA condicional | Só se útil aos cenários: área segura, limite e restauração correta; não importar comando destrutivo. |
| V-F | Lote 3, saída de combustível | Verificar necessidade/contrato; não confundir com nova física de combustível. |
| V-G | Lote 3, compat existente | Pular o que já existe; novos materiais/balanceamento ficam separados. |
| E-A | Lote 4, redstone de turbina | Recurso novo incluído no plano; implementação própria. |
| E-B | Lote 4, RPM | Medir benefício e preservar sincronização inicial/interpolação. |
| E-C | Lotes 2/4, contratos | Extrair somente interfaces/componentes úteis; sem reescrita geral. |
| E-D | Lotes 0/2/4, profiler | Instrumentação nativa para orientar decisões. |
| E-E | Fora do lote principal: múltiplos combustíveis/reações | Modelo de gameplay, save e balanceamento exigem plano próprio. |
| E-F | Lote 3, receitas próprias | Preservar a máquina e suas faces; substituir processo fixo de modo coerente. |
| E-G | Expansão separada: carregamento de itens | Não confundir com “carregamento de receitas” autorizado neste pedido. |
| E-H | Expansão separada: química Mekanism | API externa específica; não faz parte da consolidação de fluidos nativos. |
| E-I | Expansão separada: tiers e máquinas novas | Não introduzir progressão/conteúdo adicional por consequência deste plano. |

Os patches históricos B09 do dossiê são uma lista de triagem, não uma implementação adicional: fonte vazia, capacidade na GUI, API de localização, baldes e Sodium só geram alteração se existir problema equivalente no Rework atual.

## 12. Verificação geral e critérios de saída

### 12.1 Por alteração

- Teste do contrato corrigido ou recurso novo, mais regressões diretamente afetadas.
- Build e GameTests existentes após alteração de código/conteúdo relevante.
- Logs sem erros do namespace; validar assets, traduções e contagens de conteúdo modificadas.
- Conferência de dependências/imports/metadados e do JAR quando houver alteração de build ou recurso incorporado.
- Comparação do estado de referência para mudanças de física, I/O ou sincronização.
- Registrar quais verificações foram executadas e quais continuam pendentes. Sem aprovação fictícia de teste manual.

### 12.2 Matriz final

| Área | Verificação exigida |
| --- | --- |
| Ambiente limpo | Minecraft + NeoForge + Rework; sem referências instaladas, projetos irmãos, natives extras ou bibliotecas de guia. |
| Cliente e servidor | Início e uso real; classes de cliente isoladas no dedicado. GameTest sozinho não substitui interação em cliente remoto. |
| Energia | Vazio/cheio, valores grandes, transferência parcial, simulação, várias portas e consumidor variável. |
| Fluidos | Espécie, direção, componentes quando aplicáveis, parcial, saída cheia, dump, transição e circuito completo. |
| Simulação | Motores realmente disponíveis, ligar/desligar, combustível esgotado, contadores e resultados pendentes. |
| Persistência | Reinício, unload parcial/completo, desmontagem/remontagem e trabalho em andamento. |
| Dados/receitas | Reload, substituição, remoção, conflitos e entrada de cliente novo. |
| Redstone | Pulso/nível/limiares, estado desmontado, vizinhos, persistência e conflitos documentados. |
| Guia | PT-BR/inglês, resolução/escala, todas as páginas, tooltips, 3D, receitas reais e ausência de viewer. |
| Compat existente | Cada integração disponível testada separadamente e ausência de todas validada. |
| Desempenho | Cenários comparáveis, física equivalente, tráfego antes/depois e ausência de aumento desnecessário de trabalho. |
| Artefato | JAR com licença/assets, sem módulos externos embutidos, IDs locais e dependências transitivas auditadas. |

### 12.3 Entrega de cada lote

Entregar alterações locais, lista de itens implementados, itens pulados e motivo, testes executados, resultados, mudanças visíveis e pendências específicas. Atualizar `changelog.md` apenas com o que realmente entrou, e documentação de personalização/arquitetura quando seus contratos mudarem.

**Condição de conclusão:** itens selecionados implementados e validados, ou explicitamente dispensados por evidência atual. Uma hipótese inconclusiva permanece registrada como pendência, nunca convertida em correção aplicada ou em “bug ausente” sem prova.

## 13. Registro de execução — iniciar somente na tarefa de implementação

Este quadro começa sem resultados de implementação. As observações do dossiê orientam a triagem, mas devem ser revalidadas.

| Lote/item | Estado | Evidência local | Origem e adaptação | Verificação | Resultado/motivo de dispensa |
| --- | --- | --- | --- | --- | --- |
| Lote 0 | A executar | A registrar | Revisões/arquivos a conferir | Baseline a executar | — |
| B01–B08 | A verificar individualmente | Não presumir resultado histórico | Conforme fichas do plano | Antes/depois | — |
| Fluidos nativos | A inventariar | Tanques/portas atuais | Contratos locais e referências selecionadas | I/O/persistência | — |
| E-F / V-A / V-B | Planejado | Revalidar implementações existentes | Receitas/datagen/Data Maps | Dados/reload/rede | — |
| E-A / E-B | Planejado | Revalidar ausência/estado | Sensores/RPM do Extreme adaptados | Automação/rede/cliente | — |
| V-D / guia | Planejado | Plano editorial + tecnologia DFM 1.21.1 | Código interno, sem dependência | Guia completo/cliente/dedicado | — |

