# Planejamento do guia do BiggerReactors-Rework

Proposta editorial de 23/10/2026. Destino: Minecraft 1.21.1 / NeoForge.
Este documento organiza o conteúdo; não implementa o livro e não define novos recursos do mod.

## Pesquisa e limites

- A página solicitada, https://ftbwiki.org/Big_Reactors, apresentou proteção Anubis. Não foi possível extrair seu conteúdo integral nem verificar todas as páginas vinculadas.
- A pesquisa foi complementada pelas páginas acessíveis/indexadas da FTB, pelo repositório original e pela documentação do BiggerReactors. Essas fontes cobrem versões diferentes e não devem ser tratadas como documentação automática do rework.
- O código local foi consultado para identificar os blocos atuais, diferenças de montagem, receitas e configurações.
- A referência visual/editorial é o guia local do Dimensional Flux Miner: capítulos, receitas, ícones de itens, dicas, alertas, tabelas e montagem em 3D com lista de peças.
- Todo texto final deve ser reescrito para o jogador do rework. Números antigos não devem ser copiados sem conferência na versão final.

## Formato do livro

- Sumário com ícones e navegação entre capítulos.
- Uma pergunta prática por seção: o que é, para que serve, como montar, como operar ou como corrigir.
- Receitas reais do mundo/modpack, evitando imagens de receitas antigas.
- Páginas de montagem com modelo 3D, corte por camadas e lista de materiais.
- Descrições de blocos com função, posição permitida, obrigatoriedade para o exemplo, configuração, conexões e erros comuns.
- Tabelas de moderadores, bobinas e limites geradas dos dados ativos quando possível.
- Integrações opcionais identificadas e exibidas apenas quando aplicáveis.
- Valores de exemplos medidos em condições declaradas; nenhuma promessa de produção universal.

## 1. Primeiros passos

Objetivo: explicar o caminho completo antes de exigir que o jogador fabrique as peças.

- O que são reatores e turbinas multibloco.
- Caminho inicial: obter materiais, montar reator passivo, abastecer e retirar energia.
- Caminho avançado: produzir vapor, alimentar turbina e reaproveitar o fluido de saída quando aplicável.
- Energia armazenada versus geração em FE/t; quantidade de fluido versus vazão em mB/t; RPM.
- Como usar o livro, consultar receitas e reconhecer uma estrutura montada.
- Chave de Reator: mostrar apenas as interações confirmadas na implementação final.

Visual: dois fluxos, combustível → reator → FE e combustível → reator → vapor → turbina → FE.

## 2. Minérios e materiais

| Material | Conteúdo da entrada |
| --- | --- |
| Urânio | Minério comum e de ardósia profunda, pedaço bruto, processamento, pó, lingote e blocos bruto/refinado; formas realmente aceitas como combustível. |
| Grafite | Obtenção, pó, lingote, bloco, fabricação de componentes e uso como moderador. |
| Cianita | Resíduo do combustível, coleta, pó, lingote, bloco e usos em receitas. |
| Blutônio | Reprocessamento de cianita, formas disponíveis e retorno ao combustível. |
| Ludicrita | Obtenção, pó, lingote, bloco e uso em bobinas; outros usos somente quando registrados. |

Apresentar as receitas existentes, incluindo a alternativa atual de pó de urânio com areia para pó de cianita, se permanecer no rework.
Explicar que Yellorite/Yellorium são nomes históricos encontrados em tutoriais; o projeto atual usa urânio.
Altura, dimensão e frequência da geração de minério dependem da geração final do mundo e precisam de conferência específica.

## 3. Blocos do reator

| Bloco atual | Função a explicar |
| --- | --- |
| Carcaça do Reator | Estrutura externa, moldura e regras de posicionamento. |
| Vidro do Reator | Faces transparentes e lugares em que pode substituir carcaça. |
| Terminal do Reator | Acesso ao painel, estado e mensagens de montagem. |
| Barra de Combustível do Reator | Colunas internas que contêm o combustível. |
| Barra de Controle do Reator | Topo das colunas e ajuste da reação. |
| Porta de Acesso do Reator | Entrada de combustível, saída de resíduos e ejeção. |
| Tomada de Energia do Reator | Saída de FE no reator passivo. |
| Porta de Refrigerante do Reator | Entrada/saída dos fluidos no reator ativo. |
| Porta de Redstone do Reator | Sinais de comando e monitoramento disponíveis. |
| Porta de Computador do Reator | Integração opcional com computadores compatíveis. |
| Coletor de Refrigerante do Reator | Regras de conexão e efeito térmico; exemplo comparativo antes/depois. |

Cada entrada deve conter ícone, receita e exemplo de posição. Diferenciar componentes básicos do exemplo inicial dos componentes de automação e refrigeração ativa.

## 4. Montando o primeiro reator

- Dimensões externas versus espaço interno.
- Casca fechada, moldura, faces, fundo e teto.
- Colunas de combustível contínuas e barra de controle no topo de cada uma.
- Instalação de terminal, acesso e tomada de energia.
- Abastecimento, conexão a um consumidor e primeira ativação.
- Como consultar o erro quando a estrutura não monta.

Dois modelos planejados: 3×3×3 para demonstrar o mínimo e 5×5×5 para demonstrar espaço para moderadores.
O controlador atual confirma mínimo externo de 3×3×3; os modelos completos e suas listas de materiais ainda precisam ser validados no jogo.

## 5. Operação e painel do reator

- Ligar/desligar e entender aquecimento e resfriamento.
- Temperatura do combustível e da carcaça.
- Geração de energia ou fluido, consumo de combustível e reatividade.
- Combustível, resíduos, bateria e tanques internos.
- Ejeção manual e automática de resíduos.
- Inserção das barras: aumentar a inserção reduz a reação; reduzir a inserção aumenta a reação.
- Comparação entre posições das barras sob as mesmas condições.

Visual: painel anotado e explicação de cada indicador, botão e atalho existente.

## 6. Moderadores e transferência de calor

- Distinguir o material colocado no interior do reator do fluido que circula pelas portas.
- Absorção de radiação, eficiência dessa absorção, moderação e condutividade térmica, sem tratar todos os atributos como algo que deve ser maximizado isoladamente.
- Papel da distribuição das barras e do espaço entre elas.
- Exemplos presentes nos dados locais: ar, água, vidro, gelo, neve, ferro, ouro, diamante, esmeralda, grafite e ludicrita.
- Materiais de outros mods somente quando disponíveis e registrados no mundo.
- Comparação de layouts mudando uma variável por vez.

Tabela planejada: material, estado/bloco, absorção, eficiência, moderação e condutividade.
Não declarar um moderador universalmente melhor; explicar custo e efeito no desenho escolhido.

## 7. Combustível, resíduos e reprocessamento

- Fluxo principal: urânio → uso no reator → cianita → reprocessamento → blutônio → novo uso no reator.
- A recuperação é parcial; não apresentar o ciclo como combustível infinito.
- Reprocessador de Cianita: receita, entrada, saída, energia, água, progresso e automação.
- Diagnóstico: insumo insuficiente, falta de energia/água e saída cheia.

Conferência local atual: 2 lingotes de cianita produzem 1 de blutônio. O padrão configurado é 200 ticks de trabalho, com 1 FE e 1 mB de água por tick de trabalho; portanto, 200 FE e 200 mB por operação contínua concluída, sem considerar interrupções ou alterações de configuração.
A página histórica de blutônio cita 1000 mB por operação: esse valor não deve ser transferido para o rework.

## 8. Reator ativo e circuito de vapor

- Diferença entre gerar FE diretamente e transferir calor para um fluido.
- Conversão do exemplo passivo em um exemplo ativo.
- Configuração das portas de entrada e saída.
- Água → aquecimento no reator → vapor → turbina → saída do fluido condensado → retorno, para a transição água/vapor.
- Dimensionamento da alimentação e da retirada de fluidos.
- Falta de água, tanque de saída cheio e gargalos no transporte.

Regra confirmada no controlador atual: não misturar tomadas de energia e portas de refrigerante no mesmo reator.
Não presumir que todos os fluidos de mods sejam compatíveis; usar as transições registradas.

## 9. Blocos e bobinas da turbina

| Componente | Função a explicar |
| --- | --- |
| Carcaça da Turbina | Estrutura externa. |
| Vidro da Turbina | Visibilidade do rotor e posições válidas. |
| Terminal da Turbina | Painel e controles. |
| Rolamento do Rotor da Turbina | Extremidades e alinhamento do eixo. |
| Eixo do Rotor da Turbina | Linha contínua do rotor. |
| Pá do Rotor da Turbina | Aproveitamento do fluido para movimentar o rotor. |
| Porta de Fluido da Turbina | Admissão e retirada de fluidos. |
| Tomada de Energia da Turbina | Saída de FE. |
| Porta de Computador da Turbina | Controle e leitura opcionais. |
| Blocos de bobina aceitos | Extração de energia do rotor; materiais registrados, como ferro, ouro e ludicrita. |

Bobinas são feitas de blocos aceitos pelo registro; não presumir um item genérico chamado bobina.
Tabela planejada: material, eficiência, taxa de extração e bônus, com valores do mundo atual.

## 10. Montagem e operação da turbina

- Estrutura externa, dois rolamentos alinhados e eixo contínuo entre eles.
- Pás conectadas corretamente ao eixo.
- Posição das bobinas e separação entre camadas de pás e bobinas.
- Portas de fluido, tomada de energia e terminal.
- Partida, aceleração, vazão, RPM, geração e bobina engatada/desengatada.
- Modos de descarte de fluido e suas consequências, conforme controles disponíveis.
- Relação entre fornecimento de vapor, rotor, bobinas e carga.

Confirmado no código local: a validação exige exatamente dois rolamentos. A turbina precisa de um modelo próprio do rework, sem copiar a montagem antiga.
Os valores clássicos de 900/1800 RPM, 2000 mB/t ou uma quantidade fixa de pás/bobinas não são metas oficiais do rework sem teste na simulação atual.

## 11. Trocador de calor

Conteúdo adicional identificado no projeto atual, além da base histórica do Big Reactors.

- Para que serve transferir calor entre circuitos e quando usar.
- Carcaça, vidro, terminal, canal condensador, canal evaporador, porta de fluido e porta de computador.
- Montagem por camadas, conexão dos canais e separação dos circuitos.
- Identificação de entradas e saídas, temperaturas, tanques e transferência de calor.
- Apenas pares de fluidos/transições efetivamente registrados.
- Exemplo ilustrado integrado ao sistema, após validar a montagem e o funcionamento.

Confirmado no controlador atual: são necessárias quatro portas de fluido, duas associadas a canais condensadores e duas a canais evaporadores, além da presença dos dois tipos de canal.

## 12. Automação e integrações

- Entrada automática de combustível e retirada de cianita.
- Reprocessamento e retorno de blutônio.
- Transporte de água, vapor, fluido de retorno e energia.
- Configuração individual das portas e diferença entre entrada e saída.
- Redstone: documentar apenas os comandos e sensores implementados.
- Exemplo proposto de controle por bateria: ligar abaixo de um limite e desligar acima de outro, condicionado às opções disponíveis; usar dois limites para evitar alternância constante.
- CC:Tweaked: capítulo opcional com capacidades e exemplos baseados na API atual.
- Receitas e propriedades em visualizadores compatíveis, quando presentes.

Não prometer RedNet, OpenComputers, Tesla, compatibilidades de fluidos antigas ou APIs antigas apenas porque aparecem nas wikis.

## 13. Eficiência e exemplos completos

- Separar potência total, consumo por tempo e energia obtida por unidade de combustível.
- Ajustar barras, quantidade/disposição de combustível, moderadores, transferência de calor e refrigeração.
- Na turbina, comparar vazão, rotor e bobina com alimentação estável.
- Comparar produção com energia efetivamente retirada e demanda da base.
- Ensinar um teste controlado: aquecer, estabilizar, medir, mudar uma variável, repetir.

Exemplos planejados: reator mínimo, reator com moderador, conjunto reator/turbina e conjunto com trocador.
Cada exemplo deve fornecer dimensões, materiais, camadas, conexões, configurações e resultados medidos com versão/configuração declaradas.

## 14. Problemas e soluções

| Sintoma | Pontos de conferência |
| --- | --- |
| Reator não monta | Casca, dimensões, bloco inválido, terminal, colunas e barras de controle. |
| Reator ligado sem produzir | Combustível, inserção das barras, modo ativo/passivo e condições de operação. |
| Energia não sai | Tomada correta, conexão, extração e capacidade do destino. |
| Resíduo não sai | Ejeção, direção da porta e espaço no destino. |
| Reprocessador parado | Dois insumos, água, energia e espaço de saída. |
| Reator ativo para de produzir vapor | Abastecimento, temperatura e espaço no tanque de saída. |
| Turbina não monta | Dois rolamentos, alinhamento, continuidade do eixo, pás e bobinas. |
| Turbina gira mas gera pouco | Vazão, RPM, bobina, desenho do rotor e retirada de energia. |
| Trocador não monta/funciona | Canais, quatro portas, separação dos circuitos e transições compatíveis. |
| Funciona em teste e para depois | Alimentação contínua, destinos cheios e carregamento das partes relevantes do sistema. |

Associar as mensagens reais do mod a uma correção visível. Não transformar bugs históricos das wikis em defeitos confirmados do rework.

## Critérios para a redação e implementação posteriores

1. Validar os modelos no jogo e gerar as listas exatas de peças.
2. Confirmar geração de minério, nomes e funções das interações com a chave.
3. Usar receitas, registros e configuração ativos nas tabelas.
4. Conferir unidade e escala de cada indicador, incluindo temperatura.
5. Testar as integrações opcionais antes de documentá-las como disponíveis.
6. Medir exemplos de reator, turbina e trocador na simulação final.
7. Verificar riscos, danos, explosões ou radiação externa antes de afirmar qualquer comportamento de segurança.
8. Manter exemplos e conteúdos avançados do rework fora de afirmações sobre o original.

## Referências

- Página solicitada, com acesso bloqueado: https://ftbwiki.org/Big_Reactors
- Visão geral histórica: https://ftb.fandom.com/wiki/Big_Reactors
- Carcaça: https://ftb.fandom.com/wiki/Reactor_Casing_%28Big_Reactors%29
- Vidro: https://ftb.fandom.com/wiki/Reactor_Glass_%28Big_Reactors%29
- Acesso/combustível: https://ftb.fandom.com/wiki/Reactor_Access_Port
- Redstone: https://ftb.fandom.com/wiki/Reactor_Redstone_Port_%28Big_Reactors%29
- Blutônio/reprocessamento histórico: https://ftb.fandom.com/wiki/Blutonium_Ingot/en
- Repositório original: https://github.com/erogenousbeef-zz/BigReactors
- Histórico do original: https://github.com/ZeroNoRyouki/BigReactors/blob/master/Changelog.md
- Moderadores do BiggerReactors (documentação de versões anteriores): https://biggerseries.net/en/biggerreactors/reactor/moderators
- Bobinas do BiggerReactors (documentação de versões anteriores): https://github.com/Mitchell5200/BiR-Wiki/blob/master/biggerreactors/turbine/coils.md

Referências locais consultadas:

- `src/main/resources/assets/biggerreactorsrework/lang/pt_br.json`
- `src/main/java/biggerreactorsrework/Config.java`
- `src/main/java/biggerreactorsrework/multiblocks/reactor/ReactorMultiblockController.java`
- `src/main/java/biggerreactorsrework/multiblocks/turbine/TurbineMultiblockController.java`
- `src/main/java/biggerreactorsrework/multiblocks/heatexchanger/HeatExchangerMultiblockController.java`
- `src/main/java/biggerreactorsrework/machine/tiles/CyaniteReprocessorTile.java`
- `src/main/java/biggerreactorsrework/compat/computercraft/ComputerCraftCompat.java`
- `src/main/resources/data/biggerreactorsrework/ebcr/moderators/`
- `src/main/resources/data/biggerreactorsrework/ebest/coils/`
- `src/main/resources/data/biggerreactorsrework/transitions/`
- `../Dimensional-Flux-Miner/src/main/java/dimensionalfluxminer/client/screen/GuideContent.java`
