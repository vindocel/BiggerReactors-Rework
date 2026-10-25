# Bateria modular — conceito 01

Estudo visual de 25/10/2026 para BiggerReactors-Rework. Apenas conceito; nenhuma textura existente, modelo ou lógica do jogo foi substituído.

- `01-blocos-e-texturas.png`: casing desmontada, canto, aresta e face; vidro isolado e conectado; entrada e saída; terminal desligado, em espera e ativo; célula em perspectiva, frente e topo.
- `02-montagem-3x3x3.png`: montagem frontal, traseira com portas e corte ilustrativo mostrando a única célula interna.
- `prompts.md`: prompts completos usados na geração integrada de imagens.

## Direção de arte

Metal de estanho/grafite com leve tom quente, emendas escuras, barramentos de cobre e indicadores âmbar. Manter a linguagem industrial e a leitura em baixa resolução do mod. Paleta sugerida: #272C2B, #555853, #74766B, #96988B, #A97848, #E9BB62. Os tons das pranchas geradas são ilustrativos.

Portas usam setas opostas: entrada para baixo, saída para cima. Terminal usa visor apagado, barra âmbar discreta em espera e barras acesas no estado ativo. São propostas visuais, sem definir regras de operação.

## Conexões e futura produção

A casing segue o sistema existente: `disassembled`, `corner`, `frame`, `face`, com rotação das arestas e continuidade dos trilhos. O vidro deve usar as 16 combinações de vizinhança já existentes no mod e completar cantos côncavos conforme o sistema atual. Remover bordas entre vidros vizinhos; preservar apenas o contorno externo. A amostra 2×2 da primeira prancha demonstra conexão e não representa uma face da bateria mínima.

As faces externas da montagem mínima têm uma grade 3×3. A camada externa tem espessura de um bloco: 26 posições externas e uma posição interna. Distribuição proposta: terminal no centro frontal; vidro no centro direito e superior; entrada no centro traseiro; saída no centro esquerdo; casing no centro inferior e nas 20 posições de arestas/cantos. Assim o exemplo contém 21 casings, dois vidros, duas portas, um terminal e uma célula. A perspectiva e o corte são ilustrações de conceito; o modelo final deve respeitar rigorosamente a grade cúbica.

A célula é um único tipo de bloco, unindo armazenamento e transferência. Proposta de volume: gaiola cúbica com quatro suportes, três placas capacitivas empilhadas, barramentos de cobre e fendas âmbar. Fazer a futura geometria com cuboides alinhados, dentro de um bloco. Não há valores definidos de capacidade ou transferência. Quantidades e comportamento serão tratados na implementação.

Estas imagens ampliadas não são arquivos PNG 16×16 prontos para uso. Após aprovação da direção visual, produzir sprites pixel a pixel, transparência real do vidro, variantes conectadas, UVs e modelo da célula. O estado ativo do terminal pode receber animação em etapa posterior.

## Referências consultadas

Texturas locais em `src/main/resources/assets/biggerreactorsrework/textures/block/reactor` e `turbine`, estados da casing e implementação de vidro conectado do projeto; imagens Mekanism fornecidas pelo usuário.

Também foram consultados os arquivos oficiais do Mekanism, branch `1.21.x`, em 25/10/2026. Os modelos de célula e provedor usam um cubo com camadas de textura `all`, `glow` e `led`; a nova célula é uma proposta original e não uma reprodução dessa geometria ou paleta.

- https://github.com/mekanism/Mekanism/tree/1.21.x/src/main/resources/assets/mekanism/textures/block
- https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/resources/assets/mekanism/models/block/induction/cell/base.json
- https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/resources/assets/mekanism/models/block/induction/provider/base.json

