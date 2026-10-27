# Correções da auditoria de produção — 26/10/2026

Autorização: executar as correções apontadas na auditoria, sem commits. Preservado o estado local
anterior, inclusive o controle de acúmulo. Regras permanentes atualizadas no AGENTS externo.

## Implementado

1. Validação estrutural do formato binário antes das alocações: tamanhos, truncamento, profundidade,
   tipos, dados excedentes e chaves dos compostos. Limite existente de bytes do canal preservado.
2. Pacotes de interface vinculados a containerId e posição nos dois sentidos; protocolo interno 2.
3. Rods rejeitam formatos incorretos, valores não finitos e nomes excessivos.
4. Correção dos intervalos numéricos, negativos permitidos, extremos abertos e não finitos.
5. Remoção do responsável pelo salvamento permite selecionar outro bloco sobrevivente.
6. União bloqueada entre duas estruturas independentes abastecidas, conforme resposta do autor.
   A origem da máquina é persistida em suas peças para distinguir fragmentos da mesma estrutura.
   Verificados também estoques ainda pendentes no NBT. O controlador abastecido é preservado
   quando o outro está vazio. Estoques não são somados cegamente.
7. Finalização da simulação assíncrona pendente usa a mesma proteção da queima normal.
8. Reprocessadora puxa o fluido da receita e considera o fluido presente ao selecionar alternativas;
   suas receitas não entram no livro vanilla de crafting.
9. recovery configurável nos Data Maps; quente genérico mantém a exigência de origem.
10. Cache distingue snapshot vazio válido de ausência de sincronização e é limpo no logout.
11. Sprites aquecidos limitados aos perfis genéricos elegíveis. Cryo Fuel usa os recursos originais
    verificados na integração instalada: água vanilla tingida. Novos perfis podem recarregar o atlas.
12. Geração da turbina mantida como bruta, explicitada no tooltip e nas estatísticas; rotor, consumo,
    armazenamento e descarte não foram reequilibrados. Comparação com potencial preservada.
13. Removidos help.png, arrow_left.png, arrow_right.png e copper/end.png/copper/side.png do fuel rod.
    Nenhum recurso de reactor2 removido; duplicados usados e barras independentes preservados.
14. Eliminadas 63 repetições de render_type e uma chave chinesa duplicada. Catálogo chinês alinhado
    ao inglês, preservando traduções válidas e usando inglês nas ausências; sem alegar tradução nativa.
15. GameTests/template excluídos do JAR distribuído, disponíveis nos runs de desenvolvimento.
    Experimentos output/cooling-study ignorados pelo Git e mantidos no disco.

## Validação

- Build completo aprovado e 43 testes unitários aprovados, incluindo 8 regressões de binário/configuração.
- 54 GameTests aprovados tanto no ambiente básico quanto no ambiente com integrações opcionais.
- Novos GameTests: comando inválido de rod, substituição do delegado, bloqueio de união abastecida,
  recovery do Data Map, settle com saída cheia, estoque pendente de leitura e receita alternativa
  com transferência automática de lava e balanço de fluido; proteção de estoques em quarentena.
- Cliente iniciado no run do autor com 104 mods; mundo Mundo de teste carregado e categoria do
  trocador aberta. Cryo aquecido visualizado com tonalidade vermelha e Cryo frio original abaixo.
  Ausente o aviso anterior de falha ao derivar sua textura. Controle da interface interrompido pelo
  autor com Escape; nenhuma ação posterior de Computer Use foi realizada.
- Mantidos avisos de outros mods no modpack; não atribuídos a este projeto nem modificados.

## Limites e itens que não justificaram patch especulativo

- Troca rápida de menus em multiplayer: proteção por identidade implementada; não foi executada
  uma sessão com dois clientes e latência artificial.
- Recarga de JEI/EMI e troca entre servidores com datapacks diferentes: o cache foi corrigido;
  a renovação integral do visualizador ainda não foi reproduzida em todos esses cenários.
- Perfis incompatíveis em saves já possuem quarentena por fingerprint. Não foi alterada essa regra
  para reinterpretar calor/fluido antigo com coeficientes novos.
- Identidade das peças é persistida e a seleção do delegado foi testada; uma matriz prolongada de
  unload/reload de vários chunks com máquinas máximas ainda não foi executada.
- Os textos chineses herdados preservados ainda precisam de revisão por falante nativo; os novos
  textos sem tradução usam inglês, como explicitado acima.

## Entrega

Alterações somente locais, sem add/commit/push/publicação. O JAR é gerado em build/libs.

Conferência final do artefato: JAR de 2.287.705 bytes, sem classes de GameTest ou reactor2,
com licença, sem as cinco texturas retiradas. JSONs sem erros ou chaves duplicadas; os três
catálogos possuem 599 chaves. git diff --check sem erros de whitespace.
