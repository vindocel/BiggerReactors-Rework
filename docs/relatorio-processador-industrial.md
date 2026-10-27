# Processador Industrial e rendimento da Obsidiana Líquida

Implementação local de 27/10/2026. Minecraft 1.21.1, NeoForge e Java 21. JAR 0.1.3 atualizado, sem commit ou publicação.

## Correção da meta de rendimento

A calibração anterior usava vapor por combustível nuclear, que não era o critério pretendido. Esse critério foi substituído: agora cada mB de Obsidiana Líquida aquecida entrega 20% mais calor no trocador que cada mB de Cryo Fuel aquecido.

O calor por mB da obsidiana passou de 30 para **36**. Cryo Fuel permanece em **30**. Os demais coeficientes dos perfis existentes não foram alterados nesta correção. O custo de absorver o calor no reator acompanha a mesma propriedade, conservando energia; a vazão quente do reator pode mudar, portanto não se deve confundir este ganho por volume com ganho por combustível.

| Teste com lotes quentes iguais | Obsidiana Líquida | Cryo Fuel |
| --- | ---: | ---: |
| Volume quente convertido | 1.000 mB | 1.000 mB |
| Refrigerante frio devolvido | 1.000 mB | 1.000 mB |
| Calor entregue | 36.000 | 30.000 |
| Vapor produzido | **9.000 mB** | **7.500 mB** |
| Vapor por mB quente | **9** | **7,5** |

Resultado: **20% mais vapor para o mesmo volume quente convertido**. O teste usa corpos térmicos e tanques nativos com 19 canais por circuito e 18 contatos, sem perdas para o ambiente, descontando o calor inicial. Em uma montagem real, perdas ambientais, armazenamento térmico e gargalos de vazão afetam a produção observada.

Durante a validação do lote completo, foi corrigido o limite fracionário da mudança de fase: ao esgotar o volume disponível, não se aceita calor correspondente a uma fração adicional sem fluido. O teste cobre lote inteiro e último mB já parcialmente processado. Não foram alterados coeficientes de outros refrigerantes.

## Processador Industrial

- Mantém os registros e a fabricação do antigo Cyanite Reprocessor.
- Cianita e água continuam produzindo blutônio com os custos existentes.
- 1 obsidiana + 100 mB de água → 100 mB de Obsidiana Líquida, padrão de 200 ticks e 200 FE.
- Água e FE à esquerda, saída líquida à direita e slots de itens preservados.
- Saídas independentes, progresso vinculado à receita e compatibilidade com saves da máquina antiga.
- Lava comum rejeitada; pares antigos inválidos exigem limpar os tanques, sem conversão automática.

## JEI e EMI

A categoria Processador Industrial contém os dois processos. A receita de cianita mostra blutônio no slot de item. A receita de obsidiana mostra o líquido no tanque de saída, com 100 mB de água de entrada e 100 mB de produto. A água foi movida para junto da energia, acompanhando a tela da máquina.

As entradas e saídas são ingredientes reais e consultáveis nos visualizadores; a Obsidiana Líquida pode ser pesquisada como produto da máquina. O bloco é mantido como catalisador da categoria. Tempo, energia e volumes usam a receita efetiva. O circuito antigo lava → Obsidiana Líquida permanece removido.

## Validação e limites

- Build completo e 56 testes JUnit aprovados.
- 79 GameTests no núcleo e 79 com integrações opcionais aprovados.
- Testes de lotes quentes iguais, conservação térmica, circuito completo, receitas do processador e estados legados.
- JAR 0.1.3 atualizado e sem classes de teste. Código de JEI e EMI compilado contra suas APIs.
- As novas posições nos visualizadores ainda não foram inspecionadas interativamente. A instância precisa ser reiniciada para carregar as classes novas.
- A comparação anterior de +20% por combustível é histórica e não constitui o resultado desta revisão. Medições atuais completas estão em `validacao-processador-industrial.json`.
- Mudanças no perfil alteram sua identificação: estoques aquecidos do perfil anterior não são reinterpretados automaticamente; podem exigir limpeza e reabastecimento conforme a política de perfis inválidos.
