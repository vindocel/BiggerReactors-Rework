# Preenchimento visual contínuo — 26/10/2026

O autor pediu que manifolds do reator e canais do trocador não mostrem um espaço vazio quando
o estoque de um dos fluidos diminui: o outro deve ocupar o espaço restante do percurso.

## Causa

O desenho separava cada estoque pela capacidade máxima de seu tanque. Com 10% de frio e 20%
de quente, por exemplo, apenas 30% do percurso ficava preenchido. Além disso, arredondar cada
estoque em passos de 1/64 da capacidade podia fazer pequenas quantidades sumirem visualmente.

## Correção

- A divisão visual considera frio / (frio + quente) e quente / (frio + quente).
- O snapshot comum normaliza os volumes antes de arredondar, calculando a segunda parcela
  como complemento da primeira. Assim, mesmo 1 mB preenche o percurso se for o único fluido.
- O reator usa o mesmo snapshot do trocador; a coluna permanece com quente embaixo e frio em cima.
- No trocador, a divisão acompanha o comprimento inteiro, da entrada à saída, incluindo curvas,
  trechos horizontais e verticais. Os dois renderizadores também normalizam snapshots antigos.
- Quando ambos os estoques são realmente zero, não há fluido para desenhar.

Isso muda somente a representação dos tubos. Quantidades, capacidades, barras da interface,
temperaturas, conversões, queima e geração continuam usando os valores reais.

## Validação

Teste unitário cobre continuidade das colunas após quedas bruscas e estoques mínimos.
GameTest `sparseFluidStocksFillTheWholeVisualCircuit` verifica snapshot com 1 mB de cada lado,
esvaziamento de uma fase, conversão para a outra e esvaziamento completo. Para cada orientação
e curva, verifica que o volume desenhado pelos dois fluidos cobre o tubo sem sobreposição.

Comando: `gradlew.bat build runGameTestServer runGameTestCompat --offline`.
Os testes de geometria não substituem uma inspeção visual do mundo do autor.
