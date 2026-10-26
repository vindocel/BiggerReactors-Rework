# Água direta × circuito de obsidiana — reator 6×6×6

Ensaio de 25/10/2026, sem alteração de balanceamento ou dos mundos do jogador.

## Resultado

Com o mesmo desenho de reator e as mesmas inserções das rods, o circuito de obsidiana com troca direta no trocador produziu aproximadamente **73% mais vapor**, consumindo aproximadamente **14% menos combustível por tick**, que água direta. Por unidade de combustível, produziu aproximadamente o dobro de vapor.

| Circuito | Trocador externo | Canais de cada tipo | Faces diretas | Vapor médio (mB/t) | Combustível médio (mB/t) |
| --- | --- | ---: | ---: | ---: | ---: |
| Água → vapor no reator | Sem trocador | — | — | 11.081 | 2,199 |
| Obsidiana → lava → trocador | 5×5×5, geometria encontrada no save | 5 | 0 | 378 | 1,118 |
| Obsidiana → lava → trocador | 7×4×4, fileiras encostadas | 5 | 5 | 19.143 | 1,890 |
| Obsidiana → lava → trocador | 41×4×4, fileiras encostadas | 39 | 39 | 19.469 | 1,838 |

Os resultados foram confirmados no ambiente básico e no de integrações opcionais. Diferenças pequenas entre execuções decorrem do embaralhamento das barras pela simulação CPU. Os números acima foram arredondados; os JSONs guardam as médias completas.

O circuito 7×4×4 produziu **1,728 vez o vapor por tick** e **2,010 vezes o vapor por unidade de combustível** da água direta. O trocador maior chegou a **1,757 vez por tick** e aproximadamente **2,102 vezes por combustível**. O ganho do trocador maior sobre o pequeno foi de apenas cerca de 1,7% no vapor: o pequeno com contato já atendia bem esta montagem.

## Reator reproduzido

Fonte lida: `run/saves/New World`, estrutura com canto mínimo em `(-36,72,107)`. Não é a montagem 5×5×5 do outro mundo `Mundo de teste`.

- Dimensões externas: 6×6×6; interior: 4×4×4.
- 32 blocos de combustível, 8 controles, 28 manifolds e 80 faces de contato.
- Sete controles com inserção de 42%; um de 45%, na coluna interna x=2, z=0.
- Combustível e rejeito são repostos/retirados para manter estoque cheio em ambos os ensaios.

Vista superior do interior, repetida nas quatro alturas:

```text
F M F M
M F . F
F M F M
M F M F
```

`F`: combustível; `M`: manifold; `.`: espaço neutro no modo líquido. As inserções são copiadas individualmente, não substituídas por uma média. Mesmas rods não significam a mesma queima efetiva: a temperatura de operação e o controle térmico alteram o consumo, por isso o teste mede ambos.

## Disposição dos trocadores encostados

O **7×4×4 externo** tem comprimento 7, altura 4 e largura 4. O interior mede 5×2×2. Duas fileiras retas de cinco canais percorrem o comprimento, lado a lado, na mesma altura. Seção vista pela extremidade, repetida cinco vezes:

```text
Ar  Ar
 C   E
```

- `C`: canal condensador — recebe lava e devolve Liquid Obsidian.
- `E`: canal evaporador — recebe água e produz vapor.
- Cada condensador encosta em um evaporador: cinco faces de troca direta ao todo.
- Cada circuito tem uma porta em cada extremidade, totalizando quatro portas. Os fluidos não se misturam.

O **41×4×4** usa a mesma disposição, com 39 canais em cada fileira e 39 faces diretas. Foi um controle de capacidade excedente, não uma recomendação de tamanho necessário.

As geometrias térmicas foram calculadas com as mesmas regras do controlador real: capacidade dos canais, contatos diretos, contatos com o interior, massa térmica e perdas para o ambiente. São ensaios numéricos; novas estruturas não foram montadas no mundo do autor.

## Por que o 5×5×5 ficou abaixo

O save contém cinco canais condensadores e cinco evaporadores, separados por um bloco, sem contato direto. O teste usou essa geometria, alimentada com lava no primário e água no secundário. Assim, o calor precisou passar pelo interior da estrutura.

No regime estabilizado, esse trocador só devolveu cerca de **542 mB/t de obsidiana**, produzindo **378 mB/t de vapor**. A lava se acumulou no reator e no trocador, o estoque frio caiu e a proteção térmica reduziu a reação. Esse resultado demonstra um gargalo da montagem, não que a obsidiana tenha rendimento inferior quando o trocador está adequado.

No 7×4×4 encostado, a circulação média foi de aproximadamente **5.506 mB/t de obsidiana/lava**, enquanto sua capacidade ideal independente era aproximadamente **5.901 mB/t**. O reator operou sem redução automática da solicitação manual. No 41×4×4, circularam aproximadamente **5.802 mB/t**.

## Método e limites

- Classes reais de produção: `TimeSlicedReactorSimulation`, `ReactorTransitionTank`, `FluidTransitionTank` e `HeatBody`; perfis carregados pelo registro real do mod.
- 160.000 ticks de estabilização e 40.000 ticks de medição por cenário, divididos em duas janelas. Isso representa 2 h 46 min 40 s de funcionamento simulado por cenário.
- O teste exige menos de 0,5% de diferença entre as duas janelas. As execuções registradas ficaram praticamente estáveis.
- Primário fechado: uma única carga inicial de 280.000 mB de obsidiana, sem reposição externa. A soma dos estoques foi verificada a cada tick, sem perda nem criação de refrigerante.
- Água do secundário disponível e vapor retirado livremente. No teste de água direta, água reposta e vapor retirado livremente.
- Não foram simulados limites/vazões de canos externos, transporte entre chunks, custos de bombeamento ou turbina. O resultado é vapor, não uma promessa de FE/t para qualquer turbina.
- Configuração nativa usada na bancada: multiplicadores de combustível/produção 1, teto térmico 3.273,15 K; perfis atuais de água (recuperação 0,6; calor por mB 4) e obsidiana (recuperação 0,8; calor por mB 15).
- O teste não altera coeficientes para fazer a progressão passar. Ele também verifica automaticamente que os dois circuitos encostados superam água direta em vapor por tick e por combustível.

## Repetir o teste

Na raiz do projeto, execute:

```powershell
.\gradlew.bat runGameTestServer runGameTestCompat --offline
```

São servidores automatizados isolados, sem abrir nem modificar os saves do jogador. A preferência por `run` continua valendo para instâncias interativas e inspeção das montagens em jogo.

Resultados de cada execução: `run/gametest/cooling-circuit-comparison.json` e `run/gametest-compat/cooling-circuit-comparison.json`. Cópias desta medição: `output/cooling-study/comparison-6x6-core.json` e `output/cooling-study/comparison-6x6-compat.json`.

Implementação: `CoolingCircuitComparisonGameTests`. A geometria e as inserções são uma referência fixa extraída do save nesta data; mudanças futuras no reator do jogador não atualizam essa referência automaticamente.

Os testes unitários e os 33 GameTests passaram nos dois ambientes. Nenhum valor de gameplay, configuração ou mundo do autor foi alterado; sem commits.
