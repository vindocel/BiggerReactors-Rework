import re, os, sys, glob
root = sys.argv[1]
J = os.path.join(root, 'src/main/java/biggerreactorsrework')
literal = {
 'client/CommonRender.java': [("// DFM-style bar, filling the whole gauge slot.", "// Bar filling the whole gauge slot.")],
 'client/deps/RecipeViewers.java': [(", as in Dimensional Flux Miner.", ".")],
 'client/GuiArt.java': [(", like on the DFM.", ".")],
 'client/guide/GuideReferences.java': [(", as in the online guide:", ":")],
 'client/GuiTheme.java': [("with DFM's neutral-pixel darkening", "with neutral-pixel darkening")],
 'client/GuiThemePalette.java': [("CPU equivalent of DFM's dark_gui shader", "CPU equivalent of the dark GUI shader")],
 'client/GuiThemes.java': [(", as in DFM.", ".")],
 'compat/DarkModeCompat.java': [("DFM's optional Dark Mode Everywhere exclusion", "Optional Dark Mode Everywhere exclusion")],
 'compat/jade/BiggerReactorsJadePlugin.java': [("every block of an assembled battery, as in\n * Dimensional Flux Miner.", "every block of an assembled battery.")],
 'core/client/gui/RenderHelper.java': [(" (as on the DFM)", "")],
 'machine/client/CyaniteReprocessorScreen.java': [(" 2 px apart, like on the DFM.", " 2 px apart."), ("while the game runs, like on the DFM.", "while the game runs.")],
 'multiblocks/turbine/blocks/TurbineRedstonePort.java': [("/** Modified 2026-10-24: use the turbine port's own lit/unlit textures. */", "/** Uses the turbine port's own lit/unlit textures. */")],
 'client/ConnectedGlassModels.java': [("\n * Modified for the rework on 2026-10-24.", "")],
}
dates = re.compile(r" ?(Modified for the rework on|Modified|Added) 2026-10-24\.")
def load(p):
    s = open(p, newline='', encoding='utf-8').read()
    crlf = '\r\n' in s
    return s.replace('\r\n', '\n'), crlf
def save(p, s, crlf):
    if crlf: s = s.replace('\n', '\r\n')
    open(p, 'w', newline='', encoding='utf-8').write(s)
changed = set()
for rel, pairs in literal.items():
    p = os.path.join(J, rel); s, crlf = load(p); o = s
    for a, b in pairs:
        if s.count(a) != 1: print('MISS', rel, a[:50]); continue
        s = s.replace(a, b)
    if s != o: save(p, s, crlf); changed.add(p)
for p in glob.glob(J + '/**/*.java', recursive=True):
    s, crlf = load(p); o = s
    s = dates.sub('', s)
    s = s.replace('/**  */', '').replace('/** */', '')
    if s != o: save(p, s, crlf); changed.add(p)
# dead Forge-era gas compat blocks
ports = ['multiblocks/reactor/tiles/ReactorCoolantPortTile.java', 'multiblocks/turbine/tiles/TurbineFluidPortTile.java', 'multiblocks/heatexchanger/tiles/HeatExchangerFluidPortTile.java']
for rel in ports:
    p = os.path.join(J, rel); s, crlf = load(p); o = s
    s = re.sub(r"\n[ \t]*// TODO: mek gas cap registration, needs to not be a hard-dep\n(//.*\n)+", "\n", s)
    s = re.sub(r"\n[ \t]*// TODO: mek gas\n(//.*\n)+", "\n", s)
    if s != o: save(p, s, crlf); changed.add(p)
    else: print('MISS gas', rel)
p = os.path.join(J, 'core/energy/EnergyHandlerWrappers.java'); s, crlf = load(p); o = s
s = re.sub(r"\n//    private static class Mekanism \{\n(//.*\n)+", "\n", s)
if s != o: save(p, s, crlf); changed.add(p)
else: print('MISS mek wrapper')
p = os.path.join(root, 'changelog.md'); s, crlf = load(p); o = s
s = s.replace(", matching DFM's compatibility mechanism", "")
if s != o: save(p, s, crlf); changed.add(p)
else: print('MISS changelog')
for c in sorted(changed): print('changed', os.path.relpath(c, root))
