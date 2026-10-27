# Interface e carregamento da bateria — 0.1.3

- Slot superior **+**: bateria → item.
- Slot inferior **−**: item → bateria.
- Com o terminal aberto e válido, armaduras e mão secundária exibidas na lateral recebem energia automaticamente se o item aceitar FE.
- Fechar a interface interrompe esse carregamento. Os slots internos da bateria continuam funcionando normalmente.
- Equipamentos, slots e portas compartilham o limite de saída da bateria; apenas a energia aceita é descontada. IDs dos slots persistidos foram preservados.
- Corrigidos os símbolos × e → nas estatísticas. O formato binário agora escreve texto UTF-8 com comprimento explícito, mantém leitura do formato antigo e rejeita dados malformados. Cliente e servidor devem usar o protocolo interno 3.

## Validação

Testes automatizados verificam Unicode, leitura legada, rejeição de comprimentos inválidos, máquina montada, carregamento de armadura/mão secundária, limite compartilhado, fechamento da interface, bateria vazia, item sem FE e direção dos dois slots.

A validação desta alteração é automatizada; a aparência final na interface precisa ser conferida na instância reiniciada em `run`. Nenhum mundo do jogador foi alterado para executar os testes.

Resultado: build aprovado; 55 testes JUnit e 70 GameTests aprovados em cada execução (base e compatibilidade com mods opcionais). JAR 0.1.3 gerado sem classes de teste.
