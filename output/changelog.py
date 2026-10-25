p = 'changelog.md'
s = open(p, newline='', encoding='utf-8').read()
nl = '\r\n' if '\r\n' in s else '\n'
s = s.replace('; DFM-style darkening covers', '; darkening covers')
section = nl.join([
"## Battery",
" - new battery multiblock: casing, connected glass, terminal, energy ports and cells, from 3x3x3 up to 32x32x32 (configurable)",
" - each cell stores 3.2 GFE and adds 300 kFE/t to the input limit and 300 kFE/t to the output limit; capacity and limits grow linearly with the cell count",
" - an assembled battery needs a terminal, at least two energy ports and at least one cell; if every port points the same way on assembly, one is switched",
" - energy ports switch between input and output with a wrench or from their screen",
" - right-clicking any battery block other than a port opens the terminal: charge gauge, last tick's input and output, transfer limit, discharge and charge item slots, player inventory and an armor/offhand tab",
" - every cell keeps its share of the stored energy: a broken cell keeps it as an item (shown in its tooltip) and brings it into the next battery it joins",
" - connected, animated cell textures; cells light up when the battery is assembled and animate while it holds energy",
" - Jade shows the battery's total energy with the mod's energy bar on any block of an assembled battery",
" - new guide chapter with the battery's blocks, recipes, size limits and a 3D example",
"", ""])
marker = "## Cyanite Reprocessor"
assert s.count(marker) == 1 and '## Battery' not in s
s = s.replace(marker, section + marker)
open(p, 'w', newline='', encoding='utf-8').write(s)
print('ok')
