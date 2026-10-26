# Troca de refrigerante após esvaziar os tanques

O bloqueio foi reproduzido: depois de converter lava em obsidiana e extrair tudo, os dois volumes podiam estar em zero, mas `thermalRemainder` ainda guardava uma fração de energia. O preenchimento tratava essa fração como ocupação e recusava outro perfil. Os nomes do par anterior também permaneciam selecionados.

## Correção

- Entrada e saída ambas vazias limpam a seleção do par, seus nomes, componentes de origem e a fração térmica antiga.
- Isso ocorre na extração real da última unidade, no descarte manual dos dois lados e ao carregar saves que já estavam vazios. Não exige reconstruir o multibloco.
- Enquanto qualquer lado contiver líquido, a seleção continua protegida contra mistura.
- Simular extração/inserção não muda a seleção nem os estoques.
- Os dois circuitos do trocador continuam independentes: esvaziar o primário não exige esvaziar a água/vapor do secundário.
- O perfil interno do reator acompanha a limpeza do adaptador. A fração de energia de um refrigerante não passa para o seguinte.
- Não são zeradas as temperaturas da estrutura nem os contadores de transferência do tick em que ocorreu a extração.
- Fluidos salvos com perfil indisponível continuam preservados; o descarte manual de um lado não apaga o conteúdo ainda salvo no outro.

## Verificação

Os três testes iniciais falharam no código anterior, reproduzindo troca bloqueada, seleção antiga no save vazio e seleção não limpa no reator. Depois da correção, passaram:

- Os dois tipos de tanque do trocador, com sobra fracionária de calor e troca de perfil depois da última extração.
- Bloqueio mantido enquanto resta 1 mB; consultas simuladas sem alteração de estado.
- Saves antigos com zero nos dois lados; descarte manual parcial e completo.
- Troca do refrigerante do reator sem converter a fração térmica anterior em vapor gratuito.
- Cryo Fuel opcional: extração da última quantidade conserva sua identidade aquecida e permite inserir outro refrigerante no tanque vazio.
- Testes de conservação e progressão de água/obsidiana existentes continuaram passando.

Build, testes unitários e 37 GameTests passaram tanto no ambiente básico quanto no ambiente de integrações opcionais. Nenhum coeficiente de balanceamento foi alterado; sem commits.
