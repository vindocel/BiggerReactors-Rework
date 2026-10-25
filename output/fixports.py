import re, subprocess, sys
files = ['src/main/java/biggerreactorsrework/multiblocks/reactor/tiles/ReactorCoolantPortTile.java',
         'src/main/java/biggerreactorsrework/multiblocks/turbine/tiles/TurbineFluidPortTile.java',
         'src/main/java/biggerreactorsrework/multiblocks/heatexchanger/tiles/HeatExchangerFluidPortTile.java']
for f in files:
    data = subprocess.run(['git', 'show', 'HEAD:' + f], capture_output=True, check=True).stdout.decode('utf-8')
    lines = data.splitlines(keepends=True)
    out = []; i = 0
    while i < len(lines):
        l = lines[i]
        if re.match(r'\s*// (TODO|todo): mek gas', l) or (l.startswith('//') and ('GAS_HANDLER_CAPABILITY' in l or 'IGasHandler' in l)):
            # drop this line and the commented lines that follow
            i += 1
            while i < len(lines) and lines[i].startswith('//'):
                i += 1
            continue
        out.append(l); i += 1
    s = ''.join(out)
    open(f, 'w', newline='', encoding='utf-8').write(s)
    print(f, len(lines) - len(out), 'lines removed', 'MekanismGas' in s)
