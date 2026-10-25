# Célula — barrinhas animadas e conexão

Origem de trabalho: C:/Users/victo/Desktop/textures/textures/cell. A arte vermelha granulada do usuário é a referência definitiva. body.png não é regravado nem substituído.

## Alteração visual

- energy.png: três barras azuis horizontais de quatro pixels, separadas por uma linha vazia; face 16×16.
- energy_animated.png: oito quadros 16×16, em tira 16×128; preenchimento de baixo para cima com pausa e retorno. Quatro ticks por quadro, sem interpolação, como as animações em pixel art do mod.
- preview.png: composição atualizada usando o body.png real do usuário.
- preview-animated.gif: prévia ampliada; quadros idênticos podem ser combinados pelo formato GIF, preservando a duração total.
- preview-connected.png: composição isolada e painel de seis células, com contorno somente no perímetro.

A animação é visual e cíclica; os quadros não medem a carga real da bateria. A carga real continua dependendo do estado/modelo escolhido pelo código.

## Conexão

O Mekanism 1.21.x tem CTM nas células: um atlas de 32×32, metadados ctm e connect_inside=true. Seus modelos mantêm corpo, glow e led separados. Foi usada essa organização como referência, sem copiar a arte.

Há dois formatos preparados para o mesmo resultado visual:

1. ctm/body.png e body.png.mcmeta: formato do ConnectedTexturesMod, com conexão somente a biggerreactorsrework:battery_cell, independentemente do estado de carga/montagem. O caminho final esperado é assets/biggerreactorsrework/textures/block/battery/cell/ctm/body.png. O atlas usa quatro grupos: conexão completa, conexão vertical, horizontal e cantos côncavos. Requer o renderizador do ConnectedTexturesMod; metadados CTM não são interpretados pelo Minecraft sozinho.
2. connected/body_XX.png e connected/mapping.json: 47 faces 16×16 cobrindo combinações de quatro vizinhos e diagonais relevantes. Permitem integrar a conexão pelo renderizador nativo do mod, sem exigir CTM externo. O JSON documenta os bits e a orientação no UV de cada face. O renderizador precisa consultar vizinhos por face, normalizar diagonais e selecionar a variante; a camada azul permanece independente e repetida em cada célula.

A célula isolada em connected/body_00.png preserva todos os pixels do body.png. Nas demais variantes, só as três linhas externas das bordas compartilhadas podem mudar. O centro da arte e seu granulado permanecem intactos. Bordas externas são preservadas e prolongadas nos encontros. Cantos internos sem vizinho diagonal conservam o canto, evitando buracos visuais.

Não houve alteração de código do projeto, registro de blocos ou instalação de dependência. O projeto já possui bateria e conexão de vidro, mas esse renderizador de vidro não conecta automaticamente as células. Os arquivos preparam o efeito; o funcionamento no jogo precisa da integração nativa ou do ConnectedTexturesMod.

## Verificação e restauração

verification.json registra a assinatura SHA-256 de body.png, a igualdade da versão isolada, a preservação do centro em todas as 47 variantes e as dimensões das imagens. A máscara da energia foi limitada à abertura azul anterior. As prévias foram inspecionadas ampliadas sem suavização.

A pasta _backup_before_charge_bars_* criada durante a aplicação contém cópias dos arquivos existentes que foram substituídos. O resto das texturas — casing, vidro, portas, terminal — não foi alterado.

Referências primárias consultadas:
- https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/resources/assets/mekanism/textures/block/ultimate_induction_cell.png.mcmeta
- https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/resources/assets/mekanism/textures/ctm/ultimate_induction_cell.png
- https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/resources/assets/mekanism/models/block/induction/cell/base.json
- https://github.com/Chisel-Team/ConnectedTexturesMod/blob/1.21/dev/src/main/resources/assets/ctm/ctm_logic/ctm.json

A ferramenta integrada de imagens foi usada no estudo das três barrinhas. A finalização usa somente a camada azul e preserva o arquivo vermelho original; o prompt está em prompt-bars.txt.
