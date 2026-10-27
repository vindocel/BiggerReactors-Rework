# Plano do Processador Industrial e da Obsidiana Líquida aquecida

**Data:** 27/10/2026.  
**Estado:** implementação autorizada e executada em 27/10/2026; ver relatório e medições em `relatorio-processador-industrial.md` e `validacao-processador-industrial.json`. As seções abaixo preservam os critérios usados na execução.
**Revisão vigente:** ampliar e renomear o Cyanite Reprocessor existente. A proposta anterior de criar um Coolant Processor separado foi cancelada.  
**Prioridade:** fechar o circuito que permite usar lava comum como fonte térmica do trocador.  
**Alvo mantido:** Minecraft 1.21.1, NeoForge, Java 21. Sem mudança automática de versão, commit ou publicação.

## 1. Problema confirmado no código

O perfil `transitions/lava_to_obsidian.json5` associa:

```text
Reator:  Obsidiana Líquida → Lava
Trocador: Lava → Obsidiana Líquida + calor para o circuito de água/vapor
```

O trocador reconhece o tipo do fluido, não sua procedência. Lava retirada do Nether ou produzida por outra fonte pode entrar pelo mesmo caminho que a lava produzida pelo reator. Descartar a Obsidiana Líquida de saída permite continuar gerando vapor sem consumir combustível nuclear.

O objetivo desta mudança é retirar a lava comum desse par térmico padrão. O Processador Industrial ampliará o Cyanite Reprocessor existente: manterá a fabricação de blutônio e também fabricará o refrigerante frio. O aquecimento do refrigerante continuará acontecendo no reator.

## 2. Funcionamento pretendido

### Fabricação do refrigerante

```text
1 bloco de obsidiana + 100 mB de água + energia
                        ↓
               Processador Industrial
                        ↓
           100 mB de Obsidiana Líquida fria
```

### Circuito de operação

```text
                        combustível
                             ↓
Obsidiana Líquida fria → REATOR → Obsidiana Líquida aquecida
          ↑                                  ↓
          └──────────── TROCADOR ─────────────┘
                            ↑  ↓
                           água vapor → TURBINA → FE
```

A fabricação consome material e energia para abastecer o circuito. A circulação normal reutiliza o refrigerante; não exige fabricar outros 100 mB a cada passagem. Reposição só é necessária quando o jogador retira ou descarta fluido do sistema.

**Lava comum não entra nessa cadeia.** O Processador Industrial não produz fluido quente nem vapor.

### Reprocessamento preservado

```text
2 lingotes de cianita + água + energia → Processador Industrial → 1 lingote de blutônio
```

O produto existente no mod chama-se **blutônio**, não plutônio. Seu custo, tempo, saída de item e suporte às receitas configuradas serão preservados. A receita escolhida depende do item inserido, com uma única operação por vez.

## 3. Decisões confirmadas pelo autor

| Assunto | Regra |
| --- | --- |
| Máquina | Reaproveitar o Cyanite Reprocessor; não criar outro bloco. |
| Novo nome exibido | **Processador Industrial** em português e **Industrial Processor** em inglês, escolhidos pelo autor. |
| Identidade persistente | Preservar IDs internos de bloco, item e block entity para não perder máquinas existentes. |
| Entradas | Energia FE, água e o item correspondente à receita. |
| Processo de item | Manter cianita → blutônio, com sua saída de item e parâmetros atuais. |
| Processo de fluido | Acrescentar 1 obsidiana + 100 mB de água → 100 mB de Obsidiana Líquida. |
| Energia da obsidiana | Mesmo custo energético do reprocessamento padrão de cianita. |
| Rendimento térmico da obsidiana | Ajustar somente a Obsidiana Líquida para gerar 20% mais vapor no trocador por igual volume de fluido quente, comparada ao Cryo Fuel atual. O critério anterior de vapor por combustível foi corrigido pelo autor. |
| Interface | Água à esquerda, junto da energia. Novo tanque de saída líquida no lugar do antigo tanque de água. Manter entrada e saída de itens. |
| Fabricação do bloco | Reutilizar a receita existente. A receita alternativa com obsidiana no centro pertencia ao bloco separado cancelado. |
| Fluido aquecido | Obsidiana Líquida deixa de virar lava; passa a ter uma variante aquecida própria ou identificada pelo sistema genérico. |
| Mundos existentes | Circuitos antigos afetados param; reator e trocador indicam líquido inválido. O jogador precisa remover todo o conteúdo incompatível e reabastecer. |
| Expansão futura | Preparar o processamento para outras receitas, sem acrescentá-las agora. |

Unidade usada no mod: **mB**, com 1 balde = 1.000 mB. Os “100 ml” solicitados serão representados como **100 mB**.

## 4. Receita, energia e velocidade

### Receita de fabricação do bloco

Manter a receita do bloco existente, agora exibido como Processador Industrial:

```text
C I C
P F P
C R C
```

- `C`: carcaça de reator, quatro unidades.
- `I`: lingote de ferro, uma unidade.
- `P`: pistão, duas unidades.
- `F`: barra de combustível do reator, uma unidade.
- `R`: redstone, uma unidade.
- Resultado: o mesmo bloco registrado como `biggerreactorsrework:cyanite_reprocessor`, com novo nome traduzido.

A proposta anterior `P O P`, com obsidiana central para fabricar um Coolant Processor separado, foi cancelada com a reutilização do bloco existente. Não criar uma segunda receita de fabricação por consequência daquela proposta.

### Receita de processamento

O código atual usa, por padrão, **1 FE por tick durante 200 ticks**, totalizando **200 FE por operação** no reprocessamento de cianita. Seu consumo de água é 200 mB por operação, pois atualmente consome 1 mB por tick.

Para a nova receita de obsidiana, a quantidade de água é a definida pelo autor: **100 mB por operação**, independentemente desse consumo do reprocessador.

**Proposta de duração:** manter os 200 ticks da referência, com o mesmo custo energético por tick. Resultado padrão:

| Medida | Valor proposto |
| --- | --- |
| Item consumido | 1 bloco de obsidiana |
| Água consumida | 100 mB por operação |
| Refrigerante produzido | 100 mB por operação |
| Duração | 200 ticks, cerca de 10 s a 20 TPS |
| Energia | 200 FE por operação, 1 FE/t durante o trabalho |
| Produção contínua | 0,5 mB/t, ou 10 mB/s a 20 TPS |
| Para fabricar 1 balde | 10 obsidianas + 1.000 mB de água + 2.000 FE; 2.000 ticks |

Os valores acima descrevem a receita de obsidiana e derivam da configuração padrão atual, não de uma implementação já medida. A receita de cianita continua usando seus parâmetros atuais; os 100 mB da obsidiana não alteram seu consumo de água. Se o servidor alterar o custo ou a duração de referência, definir os valores efetivos de forma consistente e exibi-los no visualizador da receita. Não escolher outra receita do datapack arbitrariamente como referência energética.

Os 100 mB de água precisam ser um **custo total**, não um custo por tick. Não usar divisão inteira `100 / 200` por tick, que resultaria em consumo zero. Preferir consumo de insumos por lote e progresso energético contabilizado corretamente.

## 5. Reutilização do bloco, interface e automação

### Identidade e compatibilidade da máquina

- Preservar `biggerreactorsrework:cyanite_reprocessor` como ID do bloco e item. Renomear a apresentação pelas traduções, sem remover/recriar registros persistidos.
- Preservar a identificação da block entity e do menu, quando não houver necessidade técnica de mudança. Os nomes internos antigos podem permanecer por compatibilidade.
- Ampliar os componentes atuais de estado, container, tela, armazenamento, receitas e capacidades; não registrar um Coolant Processor adicional.
- Manter o modelo, textura, receita, loot table, tags e entrada criativa do bloco existente. Não é mais necessário criar uma textura para distinguir duas máquinas.
- Atualizar o nome visível em telas, item, guia e visualizadores. Não trocar IDs de receitas antigas apenas para acompanhar a renomeação.

### Interface aprovada

```text
┌──────────────────── Processador Industrial ──────────────────────┐
│  Energia FE   Água       Entrada de item → Saída de item   Fluido │
│    [barra]  [tanque]          [slot]          [slot]      [tanque] │
│                               [progresso]                       │
│                    [condição de funcionamento]                  │
│                     [inventário do jogador]                     │
└─────────────────────────────────────────────────────────────────┘
```

- Mover a água para o lado esquerdo, junto da barra de energia.
- O espaço antes ocupado pela água, à direita, passa a mostrar o tanque de saída de refrigerante.
- Preservar o slot de entrada e o slot de saída de itens; ajustar posições e dimensões para não sobrepor barras, slots, setas, rótulos ou abas.
- Cianita usa a saída de item. Obsidiana usa a saída de fluido. Não produzir os dois resultados na mesma operação.
- Mostrar a receita ativa, progresso, custo e motivo de pausa. A barra de fluido da direita mostra o produto armazenado, sem fingir que blutônio é fluido.
- Manter moldura, aparência configurável, janelas e controles atuais do rework.

### Seleção de receita e saídas independentes

A seleção continua baseada nas receitas e no item de entrada: cianita seleciona o reprocessamento existente; obsidiana seleciona a nova produção líquida. Uma máquina executa uma operação por vez, compartilhando água e energia.

**O bloqueio acompanha somente a saída da receita ativa:**

| Situação | Comportamento |
| --- | --- |
| Slot de blutônio cheio, tanque de fluido livre | A receita de obsidiana pode funcionar. |
| Tanque de Obsidiana Líquida cheio, slot de item livre | A receita de cianita pode funcionar. |
| Saída necessária à receita sem espaço | Pausar essa operação sem perder resultado ou desperdiçar insumos. |
| Receita trocada por troca do item de entrada | Vincular o progresso à receita; não reaproveitar trabalho de cianita como progresso de obsidiana ou vice-versa. |

Produtos antigos podem permanecer na saída enquanto outro processo usa a outra saída. Não exigir esvaziar blutônio para fabricar refrigerante, nem esvaziar refrigerante para reprocessar cianita.

### Receitas extensíveis sem quebrar as antigas

O tipo atual de receita exige um `ItemStack` de resultado e é usado pelos datapacks e visualizadores. A ampliação deve manter os JSONs existentes válidos e acrescentar uma saída de fluido opcional com quantidade e componentes.

Cada receita declara **uma saída de item ou uma saída de fluido**, com validação da exclusividade. A nova receita não pode precisar de um item fictício para representar Obsidiana Líquida.

Preservar custos e duração das receitas existentes, inclusive o consumo de fluido por tick. A receita de obsidiana precisa admitir **100 mB totais por operação**. Não redefinir o consumo das receitas antigas para introduzir o custo por lote.

Atualizar serialização, sincronização, escolha de receita, filtros de entrada, verificação de espaço e entrega do resultado de forma coerente. A disponibilidade do item/fluido e o destino correto devem ser avaliados no servidor.

### Tanques e faces

- Manter o tanque existente como entrada de água/fluido da receita; acrescentar tanque de saída separado.
- Conservar a configuração de itens e energia, inclusive ejeção de blutônio e entrada automática existentes.
- Ampliar a configuração de fluidos para entrada de água e saída de refrigerante, sem misturar os tanques.
- A face configurada como saída não injeta produto; a entrada não permite que um cano retire refrigerante do tanque errado.
- Possibilitar entrada de água e extração de produto por rotas explícitas; manter válidas as rotas antigas de entrada.
- Acrescentar ejeção automática do produto líquido, preservando os controles existentes.
- Preservar componentes dos FluidStacks e invalidar capacidades quando as rotas mudarem.
- Reutilizar as capacidades atuais de FE, água e transferência. Sugestão inicial: tanque de saída com capacidade equivalente à entrada; sem novos upgrades.

### Persistência e integridade

Máquinas já colocadas devem reaparecer como Processador Industrial, com os itens, água, energia, lados e automação que tinham. Saves sem o novo tanque carregam esse tanque como vazio. Manter a leitura do composto `cyaniteReprocessorState` e dos campos legados, ou um leitor compatível explícito.

Preservar progresso válido da receita de cianita e validar a identidade/assinatura da receita ao recarregar. Um save antigo não pode ser interpretado como uma operação de obsidiana já paga.

Na nova operação, material consumido e 100 mB entregues precisam ser contabilizados de forma consistente. Persistir o estado necessário para falta de energia, chunk unload, mudança de receita, quebra e reinício não duplicarem produto nem transferirem progresso entre processos. Simulações de capacidades não alteram recursos.

## 6. Variante quente recomendada

**Recomendação técnica:** usar `biggerreactorsrework:heated_coolant`, que já transporta a identificação do refrigerante original no componente `coolant_origin`.

- Nome apresentado: **Refrigerante aquecido — Obsidiana Líquida**, seguindo a tradução dinâmica existente.
- Textura: derivada da Obsidiana Líquida, com a tonalidade avermelhada já usada nas variantes quentes.
- Identidade completa diferente de Cryo Fuel aquecido, mesmo compartilhando o registro do fluido.
- Trocador resolve a origem e devolve Obsidiana Líquida, sem confundir os perfis.
- Fluido genérico sem origem ou com perfil inválido não é aceito como uma variante válida.
- Não criar uma variante aquecida registrada individualmente nem uma nova família de fluidos se o mecanismo atual atender aos requisitos.

Isso é uma escolha técnica proposta para implementação, não um fluido novo já disponível no jogo.

## 7. Remover a ligação com lava

1. Substituir o perfil padrão Obsidiana Líquida/lava pelo par Obsidiana Líquida/refrigerante aquecido identificado.
2. Remover a associação de lava no índice de transições quentes, incluindo a recarga de registros e snapshots do cliente.
3. Conferir Data Maps, tags, receitas geradas e aliases para não manter uma segunda associação padrão lava → Obsidiana Líquida.
4. Garantir que o trocador rejeite lava comum, por qualquer face e tanto em consulta simulada quanto em inserção real.
5. Garantir que o reator não produza mais lava nesse perfil e que o processador só fabrique refrigerante frio.
6. Remover a receita antiga dos visualizadores e do guia, sem renomear a lava vanilla ou mudar seu comportamento no mundo.

### Melhoria de rendimento aprovada para a Obsidiana Líquida

A orientação anterior de preservar os coeficientes da obsidiana foi **substituída pelo autor**: como o refrigerante agora exige fabricação com obsidiana, água e FE no Processador Industrial, seu resultado deve ser **20% mais eficiente que o Cryo Fuel existente**.

**Alterar somente a Obsidiana Líquida.** Manter os coeficientes, percentuais e comportamento de água, Cryo Fuel e sódio, além da eficiência e mecânica da turbina. Não reduzir Cryo Fuel para atingir a comparação e não alterar multiplicadores globais do reator/trocador.

#### Como avaliar os 20% — definição corrigida pelo autor

A meta é a **produção de vapor no trocador por volume de refrigerante quente convertido**. O autor esclareceu que não estava pedindo 20% mais vapor por combustível nuclear nem somente melhorar a recuperação de calor dentro do reator.

```text
Rendimento no trocador = vapor produzido / volume de refrigerante quente convertido

Rendimento da Obsidiana Líquida aquecida = 1,20 × rendimento do Cryo Fuel aquecido
```

Comparar o mesmo volume de quente efetivamente convertido, com água e extração suficientes e a mesma estrutura. Descontar calor inicial armazenado, medir em regime estável e separar calor perdido para o ambiente. A comparação não deve parecer maior porque o reator entregou mais mB/t de quente.

O consumo de combustível nuclear e o rendimento do circuito completo continuam sendo métricas secundárias de conservação/balanceamento, não o critério de aprovação dos 20%.

**Diagnóstico do código entregue antes deste esclarecimento:** obsidiana e Cryo têm calor por mB igual a 30; a recuperação no reator é 0,71765 para obsidiana e 0,59 para Cryo. A medição anterior de cerca de +20% por combustível é real, mas não valida o requisito agora esclarecido. Essa calibração foi substituída pela correção implementada: calor por mB 36 para obsidiana e 30 para Cryo; teste com 1.000 mB quentes resultou em 9.000/7.500 mB de vapor, respectivamente, sem perdas ambientais.

No modelo atual, calor por mB do perfil afeta tanto o custo de aquecimento no reator quanto a liberação no trocador. A correção precisa explicitar esse acoplamento, garantir conservação e não acrescentar calor gratuito só na saída do trocador. A relação 36 contra 30 foi implementada e validada na simulação nativa: 1,20 de rendimento térmico por volume quente. O circuito completo foi testado separadamente; perdas e vazão continuam sendo consideradas.

#### Ajuste dos parâmetros

Os valores antigos da obsidiana — `latentHeat=15`, `boilingPoint=1273.15`, condutividades `1.9/1.0` e `recovery=0.8` — ficam registrados apenas como **linha de base**, não como coeficientes obrigatórios da nova variante. Os coeficientes finais serão definidos na implementação por simulação e validação para alcançar a meta, sem inventar valores nesta etapa.

Manter `turbineMultiplier=0` para o refrigerante: ele continua transportando calor, sem uso direto na turbina. Não atribuir FE/mB ao refrigerante nem criar volume adicional para simular eficiência. O ganho vem do perfil térmico da obsidiana e de sua troca de calor.

Verificar mais de uma configuração de referência para identificar desvios da relação por efeitos térmicos e de geometria. Registrar as condições e as diferenças medidas; não prometer que toda máquina limitada por vazão exibirá exatamente 20% mais produção instantânea.

A regra antiga que colocava a obsidiana abaixo ou próxima do Cryo Fuel também fica substituída. A hierarquia dos demais perfis não autoriza modificá-los: medir o sódio com seus valores atuais e comunicar eventual conflito com a progressão anterior, sem aplicar outro rebalanceamento automaticamente.

O escopo é impedir que **lava comum alimente este circuito padrão**. Não prometer impedir receitas externas arbitrárias ou fontes de fluido quente que um modpack decida adicionar.

## 8. Mundos existentes e líquidos inválidos

**Decisão do autor:** montagens antigas nesse modo param e mostram líquido inválido. Não há conversão automática de lava para Obsidiana Líquida aquecida.

Há um detalhe crítico: o fingerprint atual considera o fluido frio e coeficientes, mas não inclui o ID da saída quente. A leitura do tanque também pode substituir uma saída antiga pela primeira saída do perfil atual. Apenas trocar o JSON poderia reinterpretar lava salva como o novo produto quente.

Portanto:

- Detectar explicitamente o par antigo salvo e o produto `minecraft:lava`, além de validar o novo perfil.
- Invalidar o circuito legado afetado tanto no reator quanto no trocador, inclusive estoques ainda pendentes de leitura da simulação/NBT.
- Não emitir nem aceitar o novo fluido aquecido enquanto o circuito inválido não tiver sido totalmente esvaziado.
- Preservar os dados incompatíveis de forma identificável, sem transmutar seu conteúdo. Usar a infraestrutura de quarentena quando aplicável.
- Mostrar **Líquido inválido — esvazie os tanques e insira um refrigerante compatível** no terminal e nas estatísticas.
- Garantir um caminho funcional de remoção/limpeza manual também para estoques em quarentena. Uma quantidade inválida oculta não pode manter a máquina bloqueada depois da limpeza explícita.
- Somente depois de zerar os estoques afetados, liberar a seleção de um par válido.
- Não invalidar água, Cryo Fuel e sódio nem transformar todas as alterações de perfil em migração automática.
- Não editar mundos do jogador nem limpar tanques como preparação oculta dos testes.

A mudança de identidade vale também para tanques/canos externos: lava neles continua sendo lava e não será aquecida por uma conversão de compatibilidade.

## 9. Guia, visualizadores e apresentação

- Guia: atualizar o nome da máquina existente e apresentar os dois processos no mesmo conteúdo, sem capítulo duplicado para um bloco novo. Explicar fabricação do refrigerante antes do circuito e distinguir fabricação de aquecimento.
- JEI/EMI: atualizar a apresentação da categoria do reprocessador para Processador Industrial e acrescentar a receita líquida. Manter as receitas antigas consultáveis e permitir consulta por obsidiana, água e Obsidiana Líquida, com quantidades, tempo, FE e resultado correto.
- JEI/EMI do reator e trocador: trocar lava pela variante aquecida identificada, incluindo seus componentes.
- Renderização: validar manifolds, canais, barras, tooltip e modelo do guia com o novo par, sem alterar a regra de preenchimento visual contínuo.
- Traduções: en_us e pt_br completos; manter paridade e política de fallback do catálogo chinês.
- Descrição pública Markdown e exemplos operacionais: substituir o circuito com lava apenas quando a implementação existir. Não apresentar o plano como recurso lançado.
- Atualizar changelog e log técnico em inglês somente com o que efetivamente for implementado e testado.

## 10. Validação obrigatória da implementação

| Caso | Resultado esperado |
| --- | --- |
| Receita completa | Consumir 1 obsidiana, 100 mB de água e o custo FE de uma operação; produzir exatamente 100 mB frios. |
| Dez operações | 10 obsidianas + 1.000 mB de água → 1.000 mB de refrigerante; custo padrão 2.000 FE. |
| Falta de qualquer entrada | Não entregar produto gratuitamente. |
| Saída cheia ou com menos de 100 mB livres | Pausar sem perda de lote nem descarte automático. |
| Simulação de transferência | Nenhuma alteração de quantidade, progresso ou energia. |
| Reload durante trabalho | Retomar consistentemente, sem duplicar custo, insumos ou saída. |
| Quebra/troca de itens/receita | Nenhuma duplicação nem reaproveitamento indevido de progresso. |
| Lava infinita + água no trocador vazio | Lava rejeitada; nenhuma produção sustentada de vapor atribuída a ela. |
| Obsidiana Líquida fria no lado quente | Rejeitada como fonte quente; não gerar calor a partir da fabricação do frio. |
| Nova variante quente | Produzida no reator e resfriada corretamente no trocador. |
| Quente genérico sem origem ou com origem errada | Rejeitado; não misturar com Cryo Fuel aquecido. |
| Canos, tanques e baldes compatíveis | Manter identificação e volume; não recuperar origem apagada por adivinhação. |
| Save antigo com lava | Reator e trocador indicam líquido inválido e não reinterpretam o estoque. |
| Limpeza do save antigo | Zerar também quarentena; aceitar novo par somente depois da limpeza completa. |
| Circuito fechado estável | Conservar refrigerante; calor e vapor dependem do aquecimento no reator. |
| Nova obsidiana versus Cryo Fuel | Vapor por mB quente convertido no trocador igual a 1,20 × Cryo Fuel, com volume equivalente, regime estabilizado e correção das perdas. O teste anterior por combustível é insuficiente. |
| Outros perfis | Água, Cryo Fuel e sódio mantêm seus coeficientes e comportamento; verificar a hierarquia resultante sem rebalanceá-los automaticamente. |
| Modos e energia | Reator sólido, turbina e bateria não recebem alterações de funcionamento. |

Adicionar regressões específicas da reutilização:

- Reprocessar cianita em blutônio com os mesmos custos e taxas anteriores.
- Alternar entre cianita e obsidiana sem reaproveitar progresso indevido.
- Manter cada receita funcionando quando somente a saída da outra estiver cheia.
- Abrir um NBT antigo com inventário, água, energia e progresso sem o campo do novo tanque.
- Validar as configurações de faces antigas e os JSONs de receitas existentes.
- Verificar que o registro continua sendo o mesmo e que não surge um bloco adicional na aba criativa.

Executar testes unitários e GameTests com máquina real, ambiente básico e integrações opcionais. Conferir build, servidor dedicado, JAR e ausência de dependências novas. Para interface e aparência, usar o cliente em `C:\Users\victo\Documents\GitHub\BiggerReactors-Rework\run`.

Ao testar sem fonte quente, distinguir ausência de produção sustentada de um pequeno transiente causado por calor que já estivesse armazenado no trocador.

## 11. Ordem proposta de implementação

1. Criar regressões da brecha de lava externa, leitura dos tanques antigos e funcionamento atual da cianita.
2. Ampliar o modelo de receitas e os tanques do bloco existente, preservando seus registros e leitura dos saves.
3. Acrescentar a receita de obsidiana, saídas independentes, progresso por receita, capacidades e automação.
4. Reorganizar a interface conforme aprovado e aplicar o nome Processador Industrial nas traduções.
5. Implementar a identidade quente da Obsidiana Líquida e o bloqueio do par antigo, sem conversão automática de lava; revisar somente a obsidiana para a meta de 20% mais vapor por igual volume quente no trocador, acima do Cryo Fuel.
6. Atualizar guia, JEI/EMI, traduções, renderização e documentação pública coerentemente.
7. Validar os dois processos, saves do reprocessador, circuito completo, rejeição de lava e limpeza de estoques inválidos.
8. Consolidar regras e log técnico com resultados reais, sem commit ou publicação.

**Entrega da execução:** Processador Industrial ampliado, novo perfil aquecido e rejeição do circuito legado implementados. Artefato 0.1.3 gerado, sem commit ou publicação. Limites e validação visual constam do relatório.
