# Bateria modular — conceito 02

Revisão de 25/10/2026 conforme a direção do usuário: mesma arte dos blocos originais do Big Reactor, casing creme claro, célula simples avermelhada e energia azul.

## Previews

- 01-comparacao-casing.png: reator, turbina e bateria com os mesmos desenhos; ampliação sem suavização.
- 02-componentes.png: vidro, portas, estados do terminal, corpo da célula, energia separada e composição.
- 03-vidro-conectado.png: exemplo 2×2 de conexão com contorno externo.
- 04-montagem-3x3x3.png: render dos sprites reais numa estrutura cúbica 3×3×3, mais corte com uma célula central. Visualização de estudo; não é captura do jogo nem modelo registrado.
- 05-celula-animada.gif: prévia ampliada da composição em quatro quadros.

## Texturas reais

Os PNGs em textures/ têm largura 16. Cada face ou quadro tem exatamente 16×16 pixels. O terminal ativo e em espera preserva as tiras verticais e metadados de animação originais; energy_animated.png tem 16×64, quatro quadros 16×16.

- casing/: corner, frame, face, disassembled. Paleta creme sobre o desenho original do reator. Não foram adicionados rebites, trilhos ou detalhes novos.
- glass/: as 16 variantes originais de vizinhança, com alpha idêntico pixel a pixel. Bordas em creme e reflexos claros.
- energy_port/: inlet e outlet, usando os símbolos distintos originais e indicadores azuis.
- terminal/: off, idle, active e respectivos metadados. Desenhos e botão laranja originais; moldura creme. Tela ativa azul; espera vermelha como no original.
- cell/body.png: corpo vermelho opaco, com recesso escuro sob a energia.
- cell/energy.png: somente pixels azuis, fundo transparente.
- cell/energy_animated.png e .mcmeta: camada azul animada, quatro quadros, quatro ticks por quadro, sem interpolação.
- cell/preview.png: composição estática para visualizar; não é uma camada adicional.

A célula é um cubo simples. Neste conceito a mesma face pode ser aplicada aos seis lados; não há geometria adicional ou capacidades programadas. A camada de energia deve ser sobreposta ao corpo na futura implementação, com tratamento próprio de emissão se desejado. O metadado não cria sozinho o modelo nem registra a animação no jogo.

A estrutura mínima tem espessura externa de um bloco e espaço interno 1×1×1. O render serve para avaliação visual e não define regras de montagem ou operação.

## Produção e referências

A ferramenta integrada de imagens foi usada para estudar a nova paleta creme e criar a célula. Na finalização, as casings foram reconstruídas por remapeamento uniforme de cores das texturas originais para garantir fidelidade ao desenho, sem redesenhar nenhum pixel. A célula gerada foi convertida para uma grade verdadeira de 16×16 e paleta curta; seus pixels azuis foram separados por máscara. Não foi usada a aparência da primeira proposta.

O azul usa os tons da barra existente do mod: #35CAE8 e #37D3F3, com sombras azuis e realce #A6EBFA. Casing: cinzas quentes próximos de #CCC9BD; célula: vinho/vermelho discreto.

Referências locais: reactor/casing, reactor/glass, reactor/access_port, reactor/terminal; turbine/casing; gui/energy/energy_shimmer_sheet.png; induction_casing.png e ultimate_induction_cell.png fornecidos pelo usuário. Mantém a pesquisa do Mekanism feita na proposta anterior.

verification.json registra as dimensões reais e a conferência da geometria da casing e alpha do vidro. build-concept.cjs contém o processo reproduzível de finalização e renderização. prompts.md contém os prompts utilizados com a ferramenta integrada.

Os arquivos são conceitos separados em output/. Nenhuma textura existente ou código do mod foi substituído.
