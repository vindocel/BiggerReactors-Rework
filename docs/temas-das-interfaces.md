# Temas das interfaces

Somente o reprocessador e os terminais do reator, da turbina e do trocador de calor têm uma aba lateral com `GuiIcon.SETTINGS`. Ela abre uma janela arrastável com **Claro**, **Escuro** e **Todas as máquinas**. O claro mantém o preenchimento original `#C6C6C6`; o escuro usa `#3F3F3F`. Os títulos e rótulos ganham contraste no escuro.

O tema escurece o fundo, os slots, os botões, os seletores, as molduras das barras, os marcadores e as setas de progresso. Usa o mesmo tratamento do DFM: tons neutros mantêm 32% do brilho, com transição suave entre saturação 0,12 e 0,35. O shader fica restrito às texturas neutras das barras; os controles desenhados em código usam o cálculo equivalente. Os preenchimentos coloridos e reflexos animados ficam fora do shader, como na barra de energia do DFM.

As molduras externas de reator, turbina, trocador e reprocessador, os ícones, os fluidos e os itens mantêm suas cores e texturas. O livro de instruções conserva seu visual próprio. As portas e controles auxiliares acompanham a preferência compartilhada, mas não mostram a aba Settings.

## Preferências

Como no Dimensional Flux Miner, as preferências pertencem ao jogador e ficam em `config/biggerreactorsrework-themes.json`, sem pacotes ou alterações no servidor.

- O padrão inicial é claro, com a opção de compartilhar ativada.
- Com **Todas as máquinas** ativado, a escolha passa a valer imediatamente para todas as telas de máquinas.
- Desativando a opção, a máquina atual mantém o visual e pode receber uma escolha independente. Máquinas sem escolha própria usam a última cor compartilhada.
- Reativando a opção, a cor exibida pela máquina atual torna-se a cor compartilhada. Escolhas individuais anteriores ficam salvas para quando o compartilhamento for desativado novamente.
- Uma máquina é identificada pelo caminho do mundo local ou endereço do servidor, dimensão e posição do bloco que abriu a tela.
- A escolha persiste ao fechar a interface e reiniciar o jogo. Arquivos inválidos causam um aviso no log e retorno ao padrão claro.

O pop-up bloqueia cliques, arrasto, rolagem, digitação e tooltips dos controles e slots atrás dele. Pode ser fechado pelo botão de fechar, pela aba ou por Escape. Tab/Shift+Tab e as setas navegam pelas quatro ações; Enter/Espaço ativam a ação focada. Arrastar o título move a janela, respeitando os limites da tela.

O botão de fechar mede 10 × 11 pixels, como o da janela de faces do reprocessador. As partes coloridas dos toggles e seletores conservam exatamente os valores RGB originais, inclusive os tons menos saturados; somente sua estrutura neutra recebe o tema. A aba Settings é desenhada no fundo da máquina, e não sobre as janelas. A janela de faces fica acima dos itens e de suas contagens, com seus tooltips acima dela; quando cobre a aba Settings, bloqueia também o clique e o tooltip dessa aba.

## Dark Mode Everywhere

`DarkModeCompat` adapta a integração do DFM: quando `darkmodeeverywhere` está instalado, envia a mensagem IMC `dme-shaderblacklist` com o prefixo `biggerreactorsrework.`. Dessa forma, o shader e a recoloração de texto externos não devem sobrepor o tema próprio. A integração é opcional, sem dependência obrigatória do mod.

## Validação

`GuiThemePreferencesTest` cobre o padrão claro, escolhas por máquina, isolamento das chaves, compartilhamento, preservação da cor ao desativá-lo e persistência por serialização. `GuiThemePaletteTest` verifica os tons exatos dos slots, bordas brancas e fundo, a preservação de cores saturadas e alfa e a transição de saturação. A compilação também verifica a integração das telas, dos eventos e do desenho do shader. A aparência e a interação no Minecraft, inclusive com Dark Mode Everywhere instalado, ainda precisam de conferência visual.
