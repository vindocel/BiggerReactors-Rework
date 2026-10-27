# Renderização dos fluidos e remontagem do reator — 27/10/2026

## Escopo

Dois pedidos tratados juntos: melhorar a compatibilidade dos líquidos com Complementary Reimagined
sem exigir mods gráficos; corrigir a perda do tipo de refrigerante após quebrar/remontar o reator.

## Renderização

O caminho anterior desenhava seis faces para cada caixa, inclusive faces internas coplanares entre
as fases e entre peças. Também forçava alpha 220 em todos os fluidos e usava entityTranslucent sem
culling para todos os materiais. Isso introduzia superfícies transparentes sobrepostas e misturas
com o vidro da máquina. Esse comportamento foi identificado no código; a contribuição exata de
cada ponto para o print do usuário não foi isolada visualmente nesta rodada.

A implementação nova calcula a superfície externa da união de caixas. Contatos parciais nas curvas
são recortados sem remover as partes expostas; conexões entre blocos omitem suas tampas internas.
A orientação dos vértices acompanha as normais externas; UVs usam coordenadas locais. O alpha do
fluido é respeitado, sem translucência artificial: tint opaco usa entityCutout, tint translúcido usa
entityTranslucentCull. Luminosidade emissiva é combinada à iluminação local. Não há shaders próprios,
chamadas diretas de OpenGL, alterações no shaderpack ou dependência de Iris para esse caminho.

Aplicado ao reator, aos canais retos/curvos do trocador e à prévia do guia. Rotor e modelos estáticos
continuam nos caminhos padrão de blocos do Minecraft; não houve mudança de geometria/rotação por
suposição nem reequilíbrio de qualquer máquina.

## Reator: defeito confirmado

O GameTest coolantIdentitySurvivesRepair falhou antes da correção: 5.000 mB frios e 1.000 mB quentes
sobreviviam à quebra/reparação, mas o tipo virava EMPTY. A simulação salvava as quantidades e o novo
wrapper não recebia os metadados atualizados do tanque anterior.

O controlador agora captura a identificação/perfil/origem junto do snapshot de volumes ao desmontar
e antes de recriar a simulação. O mesmo teste verifica quantidade preservada, tipo correto e nova
inserção aceita após o reparo.

Um tanque já salvo sem a identificação não contém dados suficientes para inferir seu líquido antigo.
A interface informa refrigerante não identificado. A recuperação segura já disponível é a limpeza
manual dos dois tanques pela porta de refrigerante, seguida de reabastecimento; não foi executado
qualquer descarte automático nem edição dos estoques dos mapas do autor.

## Ambiente e limites de validação

Ambiente encontrado: Minecraft 1.21.1, NeoForge 21.1.264, Iris 1.8.14-beta.1, Sodium 0.8.13 e
ComplementaryReimagined_r5.9.3.zip ativo no run do autor. O cliente estava aberto e foi observado.
O controle da interface foi interrompido pelo autor com Escape antes de reiniciar com o renderer
novo. Não foi feita comparação visual antes/depois da correção, nem ensaio com outros shaderpacks.
Logo, a entrega é uma melhoria do caminho de renderização padrão, não uma certificação visual de
compatibilidade com Complementary ou com todos os shaders.

Referência de pipeline consultada: https://shaders.properties/current/reference/shadersproperties/ordering/
A documentação distingue os passes de block entities translúcidos e opacos; a solução mantém
camadas vanilla e reduz a geometria interna em vez de alterar as configurações do shaderpack.

Resultados finais dos testes e build registrados ao concluir a validação automática.


## Resultado automático final

Build aprovado em 27/10/2026. 50 testes unitários aprovados, incluindo sete casos da malha externa,
cobertura parcial, tampas compartilhadas e orientação dos vértices. Todos os 69 GameTests passaram
no ambiente básico (00:23:41) e com integrações (00:24:04). A perda de identidade ao remontar deixou
de ocorrer no teste que falhava, e o esvaziamento explícito de estoque sem identificação permite
reabastecer novamente. JSONs sem erros/chaves duplicadas, catálogos de idiomas com paridade,
git diff --check sem erros; JAR 0.1.3 gerado sem classes de teste.

No reator já afetado: abrir a porta de refrigerante e usar Esvaziar Tanques descarta explicitamente
os fluidos retidos; depois inserir o refrigerante correto. Essa ação deve ser feita pelo usuário,
pois os dados antigos sem identificação não permitem recuperar o tipo com segurança. O patch
previne o problema nas próximas remontagens, mas não inventa dados ausentes no save.
