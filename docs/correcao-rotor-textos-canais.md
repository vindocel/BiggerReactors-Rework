# Rotor parado, textos e fluidos nos canais — 25/10/2026

## Causas e correções

1. **Rotor invisível parado:** o bearing agendava a geometria para dez ticks após a montagem. O controlador só transmitia novas atualizações quando o RPM mudava; depois do primeiro valor zero, o envio atrasado não acontecia. O controlador agora atende também a geometria pendente, independentemente de RPM. O cliente aceita configuração válida que chega antes do estado de bloco montado. A turbina mantém os controles e a física existentes.
2. **Caracteres Â nas telas:** 119 códigos de cor do inglês estavam corrompidos como `Â§`. Foram substituídos por escapes Unicode JSON do caractere de formatação correto. Português preservado, com paridade de chaves conferida.
3. **Canais vazios visualmente:** adicionado renderizador para os dois circuitos, usando os modelos metálicos existentes. O fluido preenche o interior reto e as curvas conforme conexões reais. Frio ocupa a parte superior e quente a inferior, com proporções normalizadas quando os dois buffers juntos excedem o espaço visual. Colunas verticais usam uma escala contínua; tubos horizontais usam a altura interna útil.

Cada canal referencia um terminal de sua estrutura. Esse terminal sincroniza duas vistas agregadas, uma por circuito, com identidade dos fluidos, componentes de origem e níveis quantizados. As vistas não são inventários. Atualizações de operação ocorrem no máximo a cada dez ticks e somente quando os valores visuais mudam; não há transmissão por canal a cada tick. Na montagem são sincronizados os metadados de posição das colunas.

## Verificação

- Build e testes unitários passaram.
- 31 GameTests passaram no servidor básico e no ambiente com integrações opcionais.
- Novo teste entrega geometria de rotor a zero RPM antes da chegada do estado de bloco montado; verifica eixos e configuração de pás sem forçar rotação.
- Teste de estrutura do trocador verifica âncora dos canais, vista 50/50, persistência da vista e conservação do estoque durante captura.
- Cliente separado de verificação carregou o mundo, JEI e renderizadores sem erros registrados no log. A conferência visual completa dos canais retos/curvos e de todas as orientações do rotor permanece pendente.
- Nenhum commit realizado.

## Correção do crash de 22:24:58

O primeiro renderizador consultava `ASSEMBLED` diretamente no canal. Diferentemente do terminal, os canais não possuem essa propriedade, causando `IllegalArgumentException` ao entrar no alcance de um trocador. O carregamento de um mundo sem canais visíveis não exercitou essa chamada.

A resolução do terminal montado agora fica em `HeatExchangerChannelTile.renderTerminal()`, compartilhada pelo renderizador e pelo teste de regressão. O teste usa os dois tipos reais de canal, confirma que não têm a propriedade, resolve o terminal montado sem exceção e verifica que uma âncora ausente impede a renderização com segurança. Nenhuma propriedade de bloco nova foi adicionada e nenhuma estrutura precisa ser reconstruída.
