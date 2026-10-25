# Texturas dos lingotes e da Wrench — 24/10/2026

Direção aprovada: seguir o estilo da imagem enviada pelo autor, com lingotes compactos, topo largo,
lateral escura e reflexos marcados. Manter verde para urânio, azul para blutônio, ciano para cianita,
cinza para grafite e roxo para ludicrita. A Wrench deve representar uma chave de boca.

Ferramenta: `image_gen` integrada. Destino dos seis PNGs: `src/main/resources/assets/biggerreactorsrework/textures/item/`.
Os arquivos finais usam 16×16 pixels e transparência. A preparação para o jogo recorta a margem
transparente e reduz com vizinho mais próximo, sem desenhar uma arte substituta por código.
Os lingotes usam o modelo padrão `item/generated`; a Wrench mantém `item/handheld`.

## Prompt base dos lingotes

Use case: style-transfer. Image 1 is the REQUIRED SHAPE AND PIXEL STYLE reference: a four-item reference showing compact Minecraft ingots. Image 2 is only the COLOR reference for this material, NOT its old narrow shape. Produce ONE standalone ingot, NOT a sheet or four variants. Match the silhouette, proportions, perspective and pixel rendering of the SILVER INGOT at TOP RIGHT in image 1: chunky compact diagonal ingot, nearly-square footprint 14 pixels wide by 13 high, wide flat top, beveled rim, distinct dark gray front/right thickness, bright angular Y-shaped reflection on the front-left lip. No elongated bar, no stretched rod. Faithful very low-resolution Minecraft pixel art on an EXACT 16x16 logical grid, enlarged nearest-neighbor if required; sharp square uniformly sized pixels, flat limited palette, 6-8 colors, NO gradients or smoothing. Center on square transparent canvas with 1 logical pixel margin. Transparent background, no background shadow, no lettering, no grid lines, no extra objects. Only one material-colored ingot matching the reference style.

Complementos por material:

- Urânio: Use yellow-green uranium: dark olive outline, grass-green body, lime top, pale yellow-green reflections.
- Blutônio: Use BLUE blutonium: dark navy outline, cobalt blue face, lighter royal-blue top, pale icy blue reflection.
- Cianita: Use CYAN cyanite: dark teal outline, cyan/turquoise face and top, very pale cyan reflection. Distinct from blue.
- Grafite: Use GRAPHITE gray: dark charcoal outline, slate gray body, medium gray top, light silver-gray reflection. No gold or yellow trim.
- Ludicrita: Use PURPLE ludicrite: dark plum outline, violet body, lavender-magenta top, pale lilac reflection.

## Prompt da Wrench

Replace the attached odd creature-like Minecraft wrench item icon with an unmistakable actual OPEN-END WRENCH hand tool. ONE standalone transparent square PNG sprite. Functional Minecraft 16x16 logical-pixel inventory texture, enlarged nearest-neighbor if necessary, strict pixel art. A diagonal steel wrench from lower-left to upper-right, 13x13 logical-pixel overall footprint, sturdy straight narrow handle and broad U-shaped open jaw at upper-right with clearly visible empty notch between two prongs. Small dark charcoal rubber grip at lower-left with tiny yellow-green accent matching a nuclear industrial mod, main tool dark steel outline, slate metal midtones and pale silver highlights. The head must read as a wrench instantly, not an animal, not a bone, not a wand, not a pickaxe. At most 7 colors, sharp square pixels, no antialiasing, no smooth gradient, no text, no border, no grid, no glow, no drop shadow, no backdrop. Transparent background.
