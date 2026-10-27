# Regressão de remontagem da turbina na 0.1.3

## Evidências

Leitura somente dos arquivos de região de Mundo de teste e New World confirmou os dois mancais
nas posições salvas. A turbina de Mundo de teste possui 564 partes com a mesma identidade; New
World possui 576 partes sem a identidade nova. Ambos têm registros de fragmentos desmontados,
alguns com snapshots dos estoques. Os mapas originais não foram editados por esta investigação.

As primeiras reproduções de montagem pequena, estoques carregados e carregamento em duas etapas
passaram. Uma sequência isolada da fila de união falhou: um controlador já absorvido era ignorado
por estar vazio, perdendo o vínculo com o proprietário atual das peças.

Uma cópia apenas dos blocos/dados da turbina de Mundo de teste foi extraída como fixture isolada.
Cinco ordens de carregamento (seeds 1, 7, 42, 91 e 150) reproduziram a falha antes da correção,
incluindo mancais em controladores diferentes. A cópia carregada em ordem regular montava,
demonstrando a dependência da ordem, não a falta física de uma peça.

## Causa e correção

A proteção recente dos estoques alterou a escolha do destino das uniões e havia antecipado a
verificação de controlador vazio. A fila podia guardar referência a um controlador que já fora
absorvido. Ignorá-lo antes de seguir mergedInto impedia reunir todos os grupos da estrutura.

O processamento agora resolve primeiro o proprietário atual. Só então verifica identidade,
estoques, estado vazio e remoção do registro. O destino é escolhido por estoque, tamanho e ordem
de criação, com desempate estável. A regra que protege máquinas independentes permanece ativa.

## Validação

A suíte possui 67 GameTests: os 54 anteriores mais 13 regressões de turbina/fila. Inclui montagem
nova, dados antigos sem identidade, snapshots da mesma origem, geometria 9x12x9 em etapas,
referência de controlador absorvido e fixture salva com ordens de carregamento diferentes.
As verificações da fixture também conferem preservação dos 4.200.000 mB de água condensada e
pelo menos 18.300.000 FE já armazenados. Resultados finais registrados após a execução.

O teste usa cópias; não reabre nem regrava os mapas originais. Nenhum commit/publicação foi feito.
O JAR mantém a versão 0.1.3 e exclui classes e fixtures dos testes. A instância precisa ser
reiniciada para utilizar a correção; não é necessário reconstruir a montagem como contorno.


## Resultado final

Build completo e 43 testes unitários aprovados. Todos os 67 GameTests passaram no ambiente básico
(23:42:26) e com integrações opcionais (23:42:49), em 26/10/2026. As cinco ordens aleatórias
que falhavam antes agora montaram e preservaram água condensada e energia armazenada. O teste
isolado da referência de controlador já absorvido também passou. O JAR 0.1.3 foi regenerado e
inspecionado: nenhuma classe de GameTest nem fixture entrou na distribuição. Não houve teste
interativo nos mapas originais nesta investigação; eles foram usados apenas para leitura.
