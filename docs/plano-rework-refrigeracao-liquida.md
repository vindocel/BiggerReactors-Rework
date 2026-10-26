# Plano de rework da refrigeração líquida

Data: 25/10/2026. Alvo: Minecraft 1.21.1, NeoForge 21.1.x, Java 21.

**Estado: planejamento para revisão. Nenhuma implementação de gameplay autorizada por este documento.** As regras abaixo consolidam as decisões do autor nesta conversa. Números de balanceamento, capacidades e temperaturas permanecem sujeitos a medição; a antiga sugestão de multiplicadores 1,5×/2×/2,25×/3× não foi aprovada e não é uma meta deste plano.

## 1. Resultado pretendido

O reator terá dois modos distintos:

| Regra | Geração direta com sólidos | Refrigeração líquida |
| --- | --- | --- |
| Interior funcional | Barras de combustível e moderadores sólidos | Barras de combustível e coolant manifolds |
| Saída útil | Energia pelas portas de energia | Vapor ou refrigerante quente pelas portas de refrigerante |
| Blocos sólidos em espaços restantes | Participam conforme suas propriedades atuais | São equivalentes ao ar para a simulação; não melhoram nem pioram o desempenho |
| Líquidos colocados como blocos no interior | Não permitidos | Não permitidos |
| Controle manual das rods | Preservado | Preservado e respeitado pela proteção automática |
| Limitação por circulação de refrigerante | Não se aplica | Redução automática da queima quando o sistema não sustenta a operação |

Combustível, geração de rejeito e sua retirada continuam existindo nos dois modos. Colocar combustível no inventário não equivale a definir sua queima por segundo. A quantidade disponível, o comando das rods e a capacidade sustentável precisam aparecer como conceitos distintos.

O modo sólido conserva seu funcionamento como referência de progressão. O circuito líquido deve oferecer maior energia útil por combustível consumido em uma montagem eficiente e permitir maior potência sustentável conforme a refrigeração melhora.

## 2. Formação e contato com os manifolds

1. O modo continua identificado pelas peças de saída da estrutura. Preservar a exclusão entre portas de energia e de refrigerante.
2. Reator líquido exige manifolds e contato direto lateral entre manifold e bloco de combustível. Contato diagonal e contato somente através de outro bloco não contam.
3. Contar as faces reais de contato. Mais faces podem aumentar a retirada de calor até os limites do fluido, da circulação e do restante do circuito.
4. Manifold distante da barra não concede refrigeração por proximidade. Pode integrar uma rede conectada, mas não inventa uma face de troca com combustível.
5. Preservar inicialmente a regra estrutural de rede de manifolds conectada ao exterior sólido; a nova regra de contato é adicional. Não exigir contato com uma porta específica.
6. Não impor como decisão aprovada que cada segmento de barra tenha quatro manifolds, nem um por segmento: a exigência é haver contato produtivo e contabilizar apenas as faces existentes. Verificar configurações com cobertura parcial nos ensaios térmicos.
7. No modo líquido, normalizar todos os moderadores sólidos permitidos no interior para as mesmas propriedades do ar, tanto na transferência térmica quanto na propagação de radiação. A posição ocupada não pode receber o bônus do material por nenhum caminho da simulação.
8. Remover a aceitação de líquidos livres na validação, considerando o estado de fluido, inclusive fluidos correntes e blocos encharcados. Não alterar o comportamento desses líquidos fora do multibloco. Uma montagem contendo líquido deve explicar a posição incompatível; não apagar o líquido automaticamente.

O refrigerante inserido pela porta fica armazenado no sistema interno. Não preenche o mundo, não escorre por espaços abertos e não pode ser coletado no interior como um bloco-fonte.

## 3. Refrigerantes selecionados e progressão

| Refrigerante frio | Saída do reator | Destino da saída | Disponibilidade |
| --- | --- | --- | --- |
| Água | Vapor de água compatível | Turbina | Núcleo do mod |
| Liquid Obsidian | Lava | Condensação no trocador, retornando Liquid Obsidian | Núcleo do mod |
| Sódio líquido do Mekanism | Sódio superaquecido líquido existente no Mekanism | Resfriamento no trocador, retornando sódio líquido | Somente com os fluidos da integração presentes |
| `ad_astra:cryo_fuel` | Refrigerante aquecido identificado como Cryo Fuel | Resfriamento no trocador, retornando o Cryo Fuel original | Somente com o fluido do Ad Astra presente |

Escada pretendida: água como entrada; Liquid Obsidian como avanço com circuito secundário; Cryo Fuel na faixa da obsidiana ou um pouco acima; sódio acima desses refrigerantes. O posicionamento numérico do Cryo Fuel será uma proposta resultante das medições, dentro dessa faixa, para revisão.

Remover os demais líquidos do conjunto padrão de refrigerantes do reator, inclusive as transições incompletas de Allthemodium/Vibranium/Unobtainium. A lista passa a ser explícita; um fluido instalado não se torna refrigerante automaticamente. Expansões futuras exigem cadastro próprio por dados.

Na integração Mekanism, resolver e conferir os IDs e tags reais da versão instalada. Utilizar as versões Fluid de sódio e sódio superaquecido; não adicionar suporte a Chemical, tubos químicos ou boiler externo como parte deste rework. A origem do sódio não importa: aceitar qualquer tanque/cano que transporte o FluidStack válido.

Ausência de mod ou fluido opcional deve eliminar a opção de registros resolvidos, guia, tabelas, receitas de visualização e seletores, sem dependência obrigatória e sem entradas quebradas. Reiniciar o jogo continua necessário para alterar a lista de mods instalados.

## 4. Circuitos e energia transportada

### 4.1 Água

`Água → reator → vapor → turbina → água recuperada → reator`.

É o primeiro circuito da progressão e dispensa trocador. A vantagem será verificada na energia final da turbina por combustível efetivamente consumido.

### 4.2 Liquid Obsidian

`Liquid Obsidian → reator → lava → trocador → Liquid Obsidian → reator`.

No outro circuito do trocador: `água → vapor → turbina → água recuperada`.

O caminho lava → Liquid Obsidian já corresponde ao sentido condensador da receita atual. Pode fornecer a carga inicial do circuito. Calor trazido por lava externa é uma fonte de energia adicional e precisa ser contabilizado: não pode aparecer no ensaio como ganho do combustível nuclear.

### 4.3 Sódio e Cryo Fuel

Usam o mesmo circuito primário fechado: frio → reator → quente → trocador → frio. A água circula separadamente pelo lado evaporador e pela turbina.

### 4.4 Contrato térmico comum

- Cada conversão deve remover do equipamento a mesma energia térmica que o fluido quente transporta, dentro das tolerâncias numéricas documentadas.
- O trocador recupera essa energia; não aplica novamente o bônus do reator. Aquecer e resfriar repetidamente uma carga não pode aumentar a energia disponível.
- Calcular a conversão pela energia efetivamente transferida, limitada por contato, diferença de temperatura, disponibilidade de frio e espaço para quente.
- Mais manifolds aumentam capacidade de retirar calor; não multiplicam diretamente a energia criada por combustível.
- Separar condutividade, energia transportada por mB, temperatura operacional e rendimento do circuito. Um fluido transportar mais calor por mB reduz a vazão necessária, mas isso sozinho não garante mais vapor por combustível.
- Manter resíduos fracionários de conversão quando necessário, evitando perda permanente nas baixas vazões e produção gratuita por arredondamento.
- Desacoplar o valor térmico físico do fluido dos multiplicadores de geração. Hoje `CoolantTank` inclui multiplicadores de saída no calor efetivo de transição, enquanto o trocador utiliza o calor original. Essa diferença precisa ser tratada explicitamente antes de calibrar ciclos fechados.
- A recuperação energética superior dos níveis avançados deve ocorrer em um ponto explícito e medido do modelo, com um teto de energia recuperável. Evitar compensar silenciosamente em reator, trocador e turbina ao mesmo tempo.

## 5. Redução automática da queima

### 5.1 Três valores diferentes

1. **Capacidade máxima da configuração:** estimativa sustentável com combustível disponível, máximo comando manual e circulação ideal para o refrigerante escolhido.
2. **Limite solicitado pelo jogador:** o que os ajustes das rods permitem. Inserir mais a barra de controle continua reduzindo a reação; não inverter esse comando.
3. **Queima atual:** consumo efetivo autorizado pela disponibilidade de combustível e pelas condições reais do sistema.

O controlador automático só reduz a solicitação manual. Não altera os ajustes persistidos das rods nem força potência acima do que o jogador escolheu. Ao cessar um gargalo, retorna gradualmente ao solicitado, respeitando limites térmicos.

Não prometer um máximo constante independente do estado nuclear: quantidade de combustível/rejeito, reatividade e temperatura influenciam a simulação atual. A interface precisa indicar as condições usadas para estimar o máximo. Durante aquecimento ou convergência, mostrar que é uma estimativa, em vez de exibir precisão falsa.

### 5.2 Entradas do controle no modo líquido

- Calor armazenado no combustível e no conjunto do reator.
- Área produtiva de contato barra/manifold e perfil do refrigerante.
- Estoque frio disponível e ocupação do tanque quente.
- Conversão térmica efetiva e tendências de circulação.
- Solicitação manual e combustível efetivamente disponível.

Uma implementação candidata calcula o teto de retirada de calor a partir da área de contato, condutividade, diferença de temperatura e limitações dos tanques. A queima deve caber no orçamento térmico sustentável. Verificar a previsão antes de aplicar calor, consumo e rejeito; não queimar primeiro e corrigir apenas o mostrador depois.

Reduzir a queima antes de ultrapassar a faixa operacional. Aplicar recuperação gradual e tolerância para pequenas oscilações, sem introduzir explosão, dano estrutural ou outro sistema de falha não aprovado. Calor já acumulado continua existindo quando a queima cai a zero.

O efeito de refrigerante quente retido será físico: ele ocupa a capacidade de saída e impede converter mais frio; a retirada de calor cai e a queima acompanha. Não reinserir o produto quente como frio nem permitir que resfrie barras indefinidamente.

Não diagnosticar um cano externo pelo nome sem evidência. O reator observa o próprio estoque e as transferências realizadas: pode afirmar que o tanque quente está cheio ou que há pouco frio; não pode afirmar que o trocador está sem água se não dispõe dessa informação.

### 5.3 Modo sólido

Preservar o modelo de geração direta e os comandos manuais. Com combustível suficiente, condições estáveis e rods liberadas, operar no máximo suportado pela configuração; com rods limitando, respeitar a escolha. Não acrescentar regras de circulação a esse modo.

A estimativa de capacidade e os avisos também devem existir nele, distinguindo limitação manual, falta de combustível, reator desligado e fase de estabilização. Não apresentar tanque de energia cheio como motivo de menor queima se a simulação vigente continuar queimando nessa situação; qualquer mudança desse comportamento exige decisão específica.

## 6. Buffers e estabilidade do circuito

O reator terá estoques de frio e quente; o trocador terá estoques de entrada/saída independentes no primário e no circuito de água/vapor. Não misturar os circuitos.

- Dimensionar capacidade pela geometria funcional e pela vazão nominal da configuração, com limites configuráveis. Não usar espaço interior vazio como reservatório extra.
- No reator líquido, relacionar o armazenamento aos manifolds, com limites de troca determinados pelas faces em contato, não apenas pela quantidade total de peças.
- Registrar capacidade e autonomia em segundos de operação nominal durante a calibração. Os valores definitivos vêm dos ensaios, não de uma constante escolhida sem medição.
- Buffers maiores suavizam oscilações e dão tempo de resposta; não corrigem uma vazão permanentemente menor que a necessidade e não geram líquido.
- Contabilizar o volume total do circuito, incluindo tanques e canos externos, ao explicar sua carga inicial no guia.
- Quente cheio, frio insuficiente e saída de vapor obstruída devem propagar a limitação através do circuito, sem perda silenciosa e sem criação de refrigerante.
- Persistir quantidades, identidade e resíduos térmicos. Formação, desmontagem e recarga de chunk não podem duplicar estoque nem apagar calor para reiniciar artificialmente a capacidade de operação.

## 7. Refrigerante aquecido genérico

Usar um fluido próprio registrado uma vez, representando o refrigerante aquecido. Um componente de dados identifica o perfil cadastrado e seu fluido de origem; não registrar um novo fluido para cada entrada de datapack.

Exemplo de apresentação: **Refrigerante aquecido — Cryo Fuel**. O trocador consulta a identidade e devolve `ad_astra:cryo_fuel`. Água e os pares nativos Liquid Obsidian/lava e sódio/sódio superaquecido preservam suas saídas específicas.

### 7.1 Cadastro por dados

O esquema precisa descrever: identificador estável, fluido frio, saída existente ou genérica, disponibilidade opcional, propriedades térmicas, parâmetros de recuperação, faixa operacional e apresentação. Valores serão validados contra limites do núcleo; não aceitar NaN, negativos ou perfis que produzam ganho térmico em ciclos fechados.

Um novo refrigerante aprovado pode ser adicionado pelo fluido de entrada e suas propriedades, sem criar arte e registro quente exclusivos. O servidor resolve o cadastro e sincroniza ao cliente os perfis disponíveis.

Recarregar dados não altera o registro de fluidos. Identidades devem continuar inequívocas após salvar, sincronizar e recarregar. Alteração de perfil com líquido já armazenado não pode recalcular seu valor de forma a gerar energia gratuita; incluir política de versão e compatibilidade do conteúdo armazenado na especificação técnica antes de implementar reload.

### 7.2 Transporte e identidade

- Preservar os componentes em consultas, inserção, extração, envio automático, tanques internos, persistência e sincronização.
- Não misturar duas origens por compartilharem o mesmo Fluid genérico. A igualdade deve considerar a identidade completa.
- Rejeitar conteúdo genérico sem origem válida e perfis ausentes com diagnóstico; não escolher outro líquido de retorno por aproximação.
- Não aceitar metadados externos como autoridade para escolher energia transportada. Consultar perfis e validar o estado no servidor.
- Testar canos e tanques relevantes, em especial transporte de fluidos do Mekanism e equipamentos usados no ambiente local. Compatibilidade visual e preservação de identidade são verificações separadas.
- O transporte genérico é um marco técnico obrigatório: se um equipamento externo perder componentes, registrar a incompatibilidade e apresentar alternativas ao autor antes de trocar a arquitetura ou cadastrar inúmeras variantes.

O suporte a componentes existe no `FluidStack` do NeoForge 1.21.1. O Rework, porém, ainda rejeita componentes no tanque de transição e usa componentes vazios em caminhos de envio. Portanto, não basta mudar a receita.

### 7.3 Aparência e inventários de receitas

Resolver nome traduzido, textura animada e cor do fluido original. Aplicar tom avermelhado preservando detalhes e transparência; validar uma composição de cor que também funcione para texturas já coloridas, como o Cryo Fuel, em vez de presumir que multiplicar por vermelho basta.

Usar a identidade completa do FluidStack nas barras, ícones, manifolds, guia e receitas visuais. Extensões de cliente da versão 1.21.1 permitem consultas de textura e cor por FluidStack; testar como cada tanque externo as utiliza. Não prometer que todos os renderizadores de outros mods apresentarão a mesma variante automaticamente.

O índice do JEI/EMI não deve ficar cheio de entradas separadas de variantes quentes. Mostrar os produtos identificados nos diagramas de processo, receitas e tooltips relevantes. A informação não pode ser escondida na hora de consultar o conteúdo de um tanque.

## 8. Visualização dentro dos manifolds

Renderizar refrigerante apenas dentro do volume visual do manifold, sem modificar o estado de fluido do mundo.

O desenho deve comunicar enchimento e estado frio/quente, coerentes com os estoques do reator. Definir uma distribuição visual consistente sem dar a impressão de que cada peça possui uma cópia integral do tanque. Não somar quantidades visuais como novos estoques.

Sincronizar um estado agregado e a configuração dos manifolds; evitar atualizar todas as peças individualmente a cada tick. A renderização deve funcionar com vários reatores e em servidor dedicado sem carregar classes gráficas no servidor.

Consultar o renderer de barras de combustível e o sistema de cores do guia como referências locais, mas implementar o desenho específico de fluido. Nenhuma imagem ou textura foi alterada neste planejamento.

## 9. Interface e motivos de limitação

Ampliar o tooltip já existente sobre o ícone e o valor de consumo de combustível.

Conteúdo previsto, com valores reais do servidor:

```text
Queima máxima da configuração: X mB/t
Limite pelas barras de controle: Y mB/t
Queima atual: Z mB/t
Condição: circulação ideal / capacidade estimada
Limitação: saída de refrigerante quente insuficiente
```

Não preencher X/Y/Z com números fictícios na implementação. Conforme a decisão posterior do autor ao solicitar o monitor de estatísticas, usar **mB/t como unidade principal** para queima e vazões em todas as máquinas, inclusive neste tooltip; essa decisão substitui a apresentação principal em mB/s solicitada antes. Estoques usam mB/B, energia usa FE e potência usa FE/t. Se houver equivalência por segundo em explicações secundárias, converter a 20 ticks de jogo por segundo e identificar a unidade. Preservar precisão interna e arredondar apenas a apresentação.

Motivos possíveis, sustentados pelos dados disponíveis: operação normal, limitação manual, pouco combustível, desligado, estabilizando temperatura, pouco refrigerante frio, tanque quente cheio, capacidade térmica de troca atingida e aproximação do limite térmico. Se vários ocorrerem juntos, mostrar a causa dominante e os fatores adicionais relevantes.

O máximo ideal não deve cair e se tornar igual à queima atual apenas porque um cano está obstruído: separar a capacidade nominal das restrições momentâneas permite ao jogador entender o gargalo.

Calcular estimativas e explicações no servidor com custo limitado, cache e invalidação por mudança de geometria/perfil/configuração. Não executar uma simulação de equilíbrio inteira por reator a cada tick nem por passagem do mouse. Comparar estimativas com ensaios estabilizados antes de chamá-las de máximos.

## 10. Balanceamento baseado em medições

### 10.1 Referência

Começar pelo reator externo 5×5×5 discutido na conversa, registrando disposição e quantidade de combustível, rods, materiais, ajustes e configuração do mod. Encontrar a melhor configuração sólida válida entre os materiais disponíveis no ambiente de referência.

Para a turbina citada como 7×5, registrar as três dimensões, rotor, pás, bobinas e faixa de operação no cenário de teste. Duas medidas não bastam para fixar uma potência. Usar uma turbina compacta eficiente e capaz de processar a vazão de referência, sem presumir que qualquer turbina pequena dará conta de qualquer reator.

Preservar uma comparação controlada com a mesma geometria de combustível e fazer outra comparação de melhor montagem válida dentro do mesmo volume externo. Assim, ocupar espaços com manifolds não favorece artificialmente uma das opções.

### 10.2 Duas medições independentes

1. **Rendimento:** FE úteis entregues / combustível efetivamente consumido. Comparar com o mesmo orçamento de combustível ou consumo médio equivalente.
2. **Potência sustentável:** maior FE/t mantido em equilíbrio térmico, respeitando circulação, temperaturas e ajuste manual.

Medir também vapor por combustível, vazão frio/quente, calor perdido, temperaturas, ocupação dos buffers, RPM e eficiência da turbina. Separar aquecimento inicial, enchimento do circuito e aceleração do rotor do regime estável.

Não contar como rendimento nuclear o calor inicial da lava, energia já armazenada na turbina ou calor remanescente de outro ensaio. Controlar ou contabilizar os estados inicial e final e o calor importado/exportado.

### 10.3 Critérios de progressão

- Água em circuito eficiente deve superar a melhor geração sólida de referência em FE por combustível.
- Liquid Obsidian com trocador deve superar água direta nessa medida.
- Sódio deve superar Liquid Obsidian e Cryo Fuel dentro da progressão aprovada.
- Cryo Fuel deve ficar na faixa da obsidiana ou ligeiramente acima; apresentar proposta medida antes de fixar o número.
- Maior contato com manifolds deve melhorar a potência sustentável quando a troca barra/refrigerante for o gargalo; pode deixar de melhorar quando o limite estiver no cano, trocador ou turbina.
- Repetir em volumes menores/maiores e com materiais opcionais de alto desempenho. Uma vantagem observada em um 5×5 não prova equilíbrio global.

Primeiro medir o comportamento atual; depois propor alterações mínimas no caminho térmico e nos perfis. Ajustar a turbina globalmente apenas se as medições demonstrarem a necessidade, registrando efeitos nos demais produtores de vapor. O rendimento de uma turbina mal dimensionada não é garantia de progressão.

## 11. Áreas técnicas afetadas

| Área | Trabalho previsto |
| --- | --- |
| `ReactorMultiblockController`, `SimulationDescription` | Formação, seleção de modo, mapa neutro de sólidos, contatos e snapshot de simulação |
| `BaseReactorSimulation`, motores CPU ativos | Caminho térmico, limite de queima antes da reação e consistência entre os motores disponíveis |
| `CoolantTank`, `ReactorTransitionTank`, `FluidTransitionTank` | Estoques, energia, conversão e identidade completa |
| Portas e `IFluidTankHandler` | Transporte e simulação de operações preservando componentes |
| `FluidTransitionRegistry`, `MaterialDataMaps` | Perfis selecionados, integrações opcionais, dados e validação de ciclos |
| `HeatExchangerMultiblockController` | Ciclos primário/secundário, buffers e energia recuperada |
| Simulação da turbina | Medição e eventual calibração fundamentada do rendimento final |
| `ReactorState` e telas de terminal | Capacidade nominal, solicitação manual, queima atual e motivos |
| Manifolds e renderização de cliente | Refrigerante visível, identidade, enchimento e calor |
| Guia próprio, `RecipeViewerData`, JEI/EMI | Novas regras, diagramas, perfis disponíveis e remoção de informações antigas |
| Configuração e traduções | Parâmetros medidos e paridade pt_br/en_us |

Os arquivos de JEI/EMI, traduções e outros recursos têm alterações locais anteriores a este plano. Inspecionar o diff novamente antes de qualquer implementação; preservar trabalho em andamento. Não tocar no protótipo `reactor2` como consequência deste rework.

## 12. Ordem de execução e critérios de saída

1. **Bancada e referência atual:** registrar revisão, configurações e montagens completas; medir sólido, água e ciclos existentes. Saída: relatório reproduzível, sem novos multiplicadores.
2. **Perfis e identidade genérica:** especificar e validar cadastro, serialização e transporte com componentes. Saída: ida e volta preservam origem, quantidade e energia, inclusive através dos equipamentos externos escolhidos.
3. **Estrutura e térmica:** remover moderação por líquidos livres, neutralizar sólidos no ativo, exigir contato e ligar troca aos manifolds. Saída: testes de geometria mostram que sólidos são neutros e diagonais não contam.
4. **Controle de queima e buffers:** integrar previsão térmica, respeito às rods, recuperação gradual e motivos. Saída: interrupção de frio ou retenção de quente reduz consumo e produção sem duplicação, sem zerar calor residual.
5. **Ciclos e balanceamento:** calibrar água/obsidiana, depois sódio/Cryo Fuel opcionais. Saída: tabelas de FE/combustível e potência sustentável demonstram a progressão, com limites documentados.
6. **Visual e interface:** apresentar frio/quente nos manifolds, nomes/texturas, tooltip de queima e monitor lateral de estatísticas em reator, trocador e turbina conforme a seção 16. Saída: cliente mostra capacidade estimada e operação real com as mesmas unidades, e dedicado funciona sem classes de cliente.
7. **Guia e verificações finais:** atualizar exemplos e receitas visuais, testar núcleo isolado e cada integração presente/ausente. Saída: documentação corresponde ao comportamento verificado e números finais são apresentados ao autor.

Detalhes novos de gameplay ou alternativas necessárias por incompatibilidade voltam ao autor antes de implementar. Este documento não autoriza iniciar nenhuma dessas etapas.

## 13. Verificação obrigatória na implementação futura

- Formação dos dois modos; recusa de fluidos livres; ausência de contato; contatos parciais e diagonais; sólidos ativos equivalentes a ar.
- Respeito aos ajustes manuais; redução/retorno automáticos; consumo e rejeito coerentes com a reação efetiva; combustível escasso.
- Frio vazio, quente cheio, interrupção/reabertura de canos, atraso de circulação, saída de vapor cheia e tanque externo que aceita apenas parte do volume.
- Conservação de refrigerante e energia em ciclos; frações de mB; perfis genéricos distintos; componentes inválidos; chamadas de simulação sem efeitos colaterais.
- Persistência, reinício, recarga de chunk, formação/desmontagem e troca de modo sem criar inventário, energia ou apagar calor.
- Ensaios de estabilidade, baixas vazões e limites numéricos; custo de estimativa/renderização em múltiplos reatores.
- Identidade, textura/cor e nome em tanque, manifold, tooltip, guia e receitas; recursos recarregados; paridade de idiomas.
- Núcleo sem mods opcionais, Mekanism isolado, Ad Astra isolado e ambos presentes. Testar os transportadores de fluidos usados, sem exigir APIs químicas.
- Build/testes relevantes, GameTests para contratos de mundo, cliente para aparência e servidor dedicado. Compilar não comprova a progressão energética.

## 14. Evidências consultadas

Código local: `gradle.properties`; `Config`; controlador e simulação do reator; tanque e portas de transição; registro de moderadores e transições; controlador do trocador; simulação moderna da turbina; estado/telas do reator; rendering comum de fluidos. A seleção de modo atual depende das portas; o tooltip existente mostra consumo em mB/t; a simulação usa temperatura/reatividade, mas não foi encontrado nela o controlador de limite térmico solicitado.

Ambiente local: Allthemodium 3.2.1, Mekanism 10.7.19.85 e Ad Astra 1.21.1-1.16.26. Allthemodium não registra os vapores exigidos pelas antigas receitas; Mekanism fornece representações Fluid de sódio e sódio superaquecido; Ad Astra contém Cryo Fuel. Revalidar IDs e versões antes da implementação.

Referências externas:

- [Mekanism: circuito de fissão e sódio](https://wiki.aidancbrady.com/wiki/Fission_Reactor_Tutorial). Referência do circuito; sua melhoria de refrigeração não equivale por si a maior energia por combustível.
- [NeoForge 1.21.1: FluidStack](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/fluids/FluidStack.java). Componentes em armazenamento e sincronização.
- [NeoForge 1.21.1: IClientFluidTypeExtensions](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java). Textura e cor consultadas por FluidStack.

Este plano registra requisitos e análise do ponto de partida. Em 25/10/2026 foi acrescentada uma bancada numérica do código atual, documentada em [simulacao-liquid-obsidian-5x5.md](simulacao-liquid-obsidian-5x5.md). Ela ainda não testa a progressão completa com turbina, o controlador futuro, o transporte da nova variante ou a renderização.

## 15. Circuitos abertos e fechados e dimensionamento — complemento do autor

Preservar todas as opções atuais da turbina, incluindo vazão, bobinas, controles/redstone e modos de descarte `OVERFLOW`, `ALL` e `CLOSED`. No código atual, esses modos tratam a água condensada. O rework não obriga retornar essa água nem remove a possibilidade de descartá-la conforme a configuração escolhida.

O jogador pode abastecer o trocador com uma fonte externa contínua/infinita de água e descartar o condensado na turbina, ou pode retornar a água e fechar esse circuito. A circulação do refrigerante primário é independente dessa escolha. Fonte infinita de água não fornece calor: vapor depende da energia entregue ao trocador.

Incluir no guia dimensionamento por faces reais de troca e vazões, com os dois circuitos. O estudo numérico inicial mediu, para um reator 5×5×5 com 15 blocos de combustível, cerca de 66,31 B/s de lava em circulação ideal. No circuito fechado com trocador 4×4×5 de 3 canais por tipo, mediu aproximadamente 64,21 B/s de retorno e 220,67 B/s de vapor; com 4×4×6 e 4 canais por tipo, aproximadamente 65,03 B/s e 224,21 B/s. O 4×4×4 foi insuficiente para sustentar o mesmo regime ideal.

Esses resultados são do modelo atual, com temperaturas elevadas e sem redução automática. São referência para calibrar o futuro rework, não números finais aprovados. O relatório registra geometria, consumo, buffers, capacidade nominal versus circuito acoplado e as verificações numéricas realizadas.

## 16. Monitor lateral de estatísticas — complemento do autor

### 16.1 Objetivo e referência visual

Adicionar um botão lateral de **Estatísticas** aos terminais do reator (líquido e sólido), trocador de calor e turbina. Ao clicar, abrir uma janela de leitura com informações úteis para dimensionar o circuito. Os controles principais existentes permanecem onde estão; abrir o monitor não altera operação, vazão, rods ou descarte.

Referência consultada em leitura no DFM 1.21.1:

- `client/screen/MinerScreen.java`: instancia a janela de detalhes e fornece as linhas de dados.
- `client/screen/MachineTabs.java`: botão lateral de detalhes usando o ícone de estatísticas.
- `client/screen/MachineWindows.java`: `InfoWindow`, janela de leitura com conteúdo fornecido dinamicamente.

Adaptar esse padrão ao sistema interno de janelas/abas, à moldura e aos ícones do Rework. Não depender do DFM instalado, não alterar seu projeto e não importar sua tela inteira. Prever rolagem ou paginação se as informações ultrapassarem a área disponível; não copiar a altura crescente da janela de referência sem verificar telas pequenas e escala de GUI.

### 16.2 Unidade e significado comuns

**Vazões e queima: mB/t. Potência: FE/t. Estoques/capacidades: mB ou B, sempre com unidade explícita.** Um balde equivale a 1.000 mB. Taxa por tick e capacidade total não podem receber o mesmo rótulo.

Separar em todas as máquinas:

1. **Estrutura/capacidades:** o que foi montado e quanto pode armazenar.
2. **Conversão/produção potencial:** capacidade nominal estimada que essa configuração/perfil pode sustentar nas condições de referência identificadas, sem gargalos externos.
3. **Conversão/produção atual:** o que está sendo efetivamente consumido, convertido ou produzido e o motivo de operar abaixo do potencial.

Capacidade ideal não vira zero porque a máquina está desligada ou sem fluxo. Produção atual pode ser zero nessas situações. Estimativa ainda não resolvida deve aparecer como “Calculando” ou “Aguardando refrigerante”, nunca como um máximo inventado. Usar arredondamento coerente e evitar sinalizar gargalos criados apenas pela precisão visual.

**Escopo local obrigatório:** cada monitor mostra somente estrutura, estoques, conversões, produção e limitações da própria máquina. O reator não informa tamanho necessário de trocador, capacidade de outra máquina ou recomendações de montagem do restante do circuito. O trocador apresenta sua capacidade independente; o usuário compara os números e escolhe aumentar a estrutura. Exemplos de comparação na documentação não são linhas adicionais nos monitores.

Rótulos e nomes de entrada/saída devem acompanhar o perfil reconhecido. Não escrever “conversão de água” como título fixo do reator: água → vapor, Liquid Obsidian → lava, sódio líquido → sódio superaquecido líquido e Cryo Fuel → refrigerante aquecido — Cryo Fuel são pares distintos do mesmo painel. Motivos de desempenho reduzido também precisam corresponder à máquina; não importar “falta de energia” da mineradora como condição de um gerador que não consome energia para operar.

### 16.3 Reator líquido

Informações previstas:

| Informação | Uso para o jogador |
| --- | --- |
| Dimensões, colunas e quantidade de blocos de combustível | Identificar a configuração analisada |
| Capacidade total de combustível, combustível armazenado e rejeito | Separar estoque de consumo |
| Quantidade de manifolds e faces produtivas de contato | Entender a capacidade de troca instalada |
| Refrigerante reconhecido e produto quente | Mostrar por exemplo “Liquid Obsidian → Lava”, incluindo identidade de variantes genéricas |
| Capacidade dos tanques frio/quente e ocupação atual | Dimensionar carga e reconhecer saturação |
| Queima máxima estimada e condições de referência | Saber o potencial da configuração com combustível e circulação suficientes |
| Limite pelas rods e queima atual | Distinguir escolha manual de limitação automática |
| Conversão potencial: consumo de frio e produção do quente reconhecido, em mB/t | Mostrar o que esta configuração de reator pode converter |
| Conversão atual e taxas reais de entrada/saída, com os mesmos fluidos e unidades | Mostrar o que está efetivamente sendo convertido e movimentado no reator |
| Temperatura atual/faixa operacional e capacidade térmica em uso | Entender por que o controle está reduzindo a reação |
| Estado e motivo da limitação | Orientar a correção do gargalo observado localmente |

Após reconhecer um refrigerante, mostrar as estimativas para a montagem/perfil atual sem exigir que o usuário force o reator até o limite para descobrir sua demanda. A capacidade de combustível pode ser mostrada antes de inserir qualquer líquido; estimativas dependentes do perfil aguardam sua identificação.

A taxa nominal de quente deve corresponder à mesma condição da queima nominal exibida. Não combinar uma queima medida em uma condição com uma conversão máxima obtida em outra e apresentar ambas como um único ponto operacional.

Exemplo de conteúdo dinâmico do painel, com valores X/Y substituídos exclusivamente por dados calculados:

```text
Conversão: Sódio líquido → Sódio superaquecido líquido
Potencial: X mB/t de sódio → X mB/t de sódio superaquecido
Atual: Y mB/t de sódio → Y mB/t de sódio superaquecido
Limitação: pouco refrigerante frio
```

A igualdade volumétrica no exemplo segue o par atual 1:1; qualquer perfil futuro com outra proporção deve mostrar os volumes reais de cada lado. Água só aparece nesses campos quando for o refrigerante reconhecido. Nenhuma linha do reator calcula ou sugere o tamanho de um trocador ou de uma turbina.

No modo sólido, reaproveitar informações de estrutura, estoque, queima, limite manual, geração estimada/atual e motivos pertinentes. Não mostrar campos de refrigeração como exigências desse modo.

### 16.4 Trocador de calor

Reconhecer o fluido quente e seu retorno frio no circuito primário, e água/vapor no secundário. Não exigir circuito de água fechado para calcular ou apresentar a capacidade.

Mostrar:

- Dimensões, quantidade de canais de cada tipo e faces reais de contato entre eles.
- Pares de conversão identificados e capacidades/ocupações dos quatro tanques.
- **Conversão potencial do primário**, em mB/t: capacidade nominal estimada desta estrutura para converter o quente reconhecido em seu frio, por exemplo lava → Liquid Obsidian, sob alimentação adequada de quente e água e saídas livres.
- Demanda nominal de água e produção nominal estimada de vapor no mesmo ponto de operação.
- **Conversão atual do primário**, com os mesmos fluidos e unidades da potencial; vapor potencial/atual e taxas efetivas de entrada/saída. Entrada pela porta não é prova de conversão: o estoque pode estar enchendo.
- Temperaturas dos dois circuitos e motivo de limitação: falta de calor de entrada, pouca água, retorno frio bloqueado, vapor retido, aquecimento inicial ou capacidade térmica de troca atingida.

O objetivo é permitir comparar “o reator pode enviar X mB/t” com “este trocador consegue converter Y mB/t”. O jogador decide aumentar canais/contato ou reduzir a queima. Não atribuir à máquina conhecimento do reator remoto ou de todos os canos da rede: a comparação é possível pelos valores de cada monitor, sem inventar uma ligação automática entre equipamentos.

Apresentação prevista: “Conversão: Lava → Liquid Obsidian”, “Potencial: X mB/t → X mB/t”, “Atual: Y mB/t → Y mB/t”, além dos dados locais do circuito de água/vapor. O potencial depende da própria geometria e do perfil; a falta momentânea de lava reduz a taxa atual, sem apagar a capacidade potencial.

### 16.5 Turbina

Vapor é processado pela turbina; ela não queima urânio. As informações úteis são:

- Dimensões, rotor/pás e bobinas instaladas.
- Capacidade dos tanques de vapor e água, ocupação e armazenamento de energia.
- **Limite de vazão da estrutura**, **vazão selecionada pelo jogador** e **vazão efetivamente processada**, todas em mB/t.
- Capacidade de aproveitamento eficiente pelo rotor no regime de referência, quando validada pela simulação; não confundir limite estrutural com consumo eficiente.
- RPM atual, eficiência do rotor, **produção potencial** e **produção atual** em FE/t. A potencial é uma estimativa nas condições de referência de alimentação e regime do rotor/bobinas explicitadas; falta momentânea de vapor limita a atual e não redefine o potencial como zero.
- Água efetivamente produzida, devolvida e descartada, para dimensionar o retorno ou a reposição externa.
- Estado das bobinas, modo de descarte vigente e motivo local de desempenho reduzido.

O código atual já fornece separadamente `flowRateLimit()`, `nominalFlowRate()` e `flowLastTick()`, mas `TurbineState.flowRate` sincroniza apenas a vazão escolhida. Ampliar o snapshot: um campo com o nome “flowRate” não pode servir de prova da vazão real nem de máximo eficiente.

Não prometer FE/t fixos só por informar entrada de vapor. Rotor, bobinas, RPM, vazão ajustada e perdas alteram o aproveitamento. Não mudar o comportamento de descarte, redstone ou bobinas ao adicionar estatísticas.

### 16.6 Exemplo de dimensionamento com os dados já medidos

Valores arredondados da bancada atual, apenas para demonstrar a comparação que o monitor permitirá; não são números finais do rework.

| Monitor | Informação de referência |
| --- | --- |
| Reator 5×5×5, 15 blocos de combustível | Capacidade de combustível: 60.000 mB; circulação ideal de obsidiana/lava: aproximadamente 3.315,53 mB/t, queimando aproximadamente 1,92 mB/t |
| Trocador 4×4×4, 2 canais por tipo | Conversão potencial de lava → Liquid Obsidian: aproximadamente 2.393,00 mB/t |
| Trocador 4×4×5, 3 canais por tipo | Conversão potencial de lava → Liquid Obsidian: aproximadamente 3.563,31 mB/t |
| Trocador 4×4×6, 4 canais por tipo | Conversão potencial de lava → Liquid Obsidian: aproximadamente 4.732,25 mB/t |

Na operação acoplada real simulada com 4×4×6, os monitores mostrariam aproximadamente **3.251,40 mB/t de lava/retorno** e **11.210,32 mB/t de vapor**, com queima aproximada de **1,94 mB/t**, além dos valores nominais independentes. O usuário dimensionaria a turbina para essa vazão de vapor, verificando também o aproveitamento eficiente do rotor.

O monitor precisa deixar clara a diferença entre essas duas comparações: a capacidade do trocador com entradas ideais não é a vazão que o reator conectado necessariamente fornecerá. Temperaturas e ocupação dos estoques alteram o regime de equilíbrio.

### 16.7 Cálculo, sincronização e validação

- Produzir estatísticas a partir do mesmo modelo e dos mesmos perfis usados pelo controle, sem tabelas de máximos fixos escritas à mão para cada dimensão.
- Estimativas de regime permanente exigem cálculo/validação térmica; não usar a produção do último tick como “máximo”. O trocador precisa considerar os dois circuitos e as perdas, não apenas contar canais.
- Resolver capacidade nominal com orçamento de processamento limitado, cache e invalidação por mudança de geometria/perfil/configuração. Definir uma revisão dos dados para não misturar números de configurações antigas e atuais.
- Sincronizar um snapshot autoritativo com capacidades, taxas, estado do cálculo e motivos. Médias curtas podem estabilizar os números visuais, mas avisos de falta/bloqueio devem refletir condições reais sem atraso excessivo.
- Registrar taxas de fluxo efetivo nos caminhos de inserção/extração; consulta simulada não conta como transferência real. Conversão térmica e movimentação pela porta são métricas distintas.
- O painel aberto não acelera, reduz ou reinicia a máquina; fechar/reabrir não altera estado de gameplay. Funcionamento em servidor dedicado não depende das classes da janela.
- Validar escala pequena/grande de GUI, nomes extensos/localização pt_br/en_us, identidade de refrigerante genérico, perfil ausente, máquina desligada e estimativa ainda não calculada.
- Trocar o refrigerante reconhecido e verificar atualização do par, nomes e taxas potencial/atual; não deixar campos fixos de água nem estatísticas de outro equipamento no monitor local.
- Comparar previsões com a bancada numérica e montagens no jogo antes de rotular valores como máximos sustentáveis. Testar especificamente nominal diferente de atual, limite manual e turbina com limite estrutural maior que o aproveitamento eficiente.

O monitor passa a integrar a etapa de interface do rework; o pedido atual continua sendo planejamento. Nenhum botão, pacote de rede, estimador ou janela foi implementado nesta tarefa.
