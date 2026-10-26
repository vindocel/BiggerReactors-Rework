import json
K='recipe_viewer.biggerreactorsrework.stat.'
en={
'place':'Value: %s (#%s of %s)',
'average':'◆ Average among the options',
'moderation':'Moderation',
'moderation.high':'▲ Softens radiation, so the next fuel rods absorb more of it',
'moderation.low':'▼ Barely softens radiation; hard radiation passes the fuel rods',
'absorption':'Absorption',
'absorption.pro':'+ Turns more of the passing radiation into casing heat',
'absorption.con':'− Less radiation is left for the next fuel rods',
'efficiency':'Heat efficiency',
'efficiency.high':'▲ Turns most of the absorbed radiation into heat',
'efficiency.low':'▼ Wastes much of the absorbed radiation',
'conductivity':'Conductivity',
'conductivity.high':'▲ Moves heat from the fuel rods to the casing quickly',
'conductivity.low':'▼ Moves heat slowly; the fuel rods run hotter',
'coil_efficiency':'Efficiency',
'coil_efficiency.high':'▲ Turns more of the rotor’s work into FE',
'coil_efficiency.low':'▼ Loses more of the rotor’s work',
'bonus':'Bonus',
'bonus.high':'▲ Raises the output exponent; the gain grows with rotor speed',
'bonus.low':'▼ Little or no output bonus',
'extraction':'Extraction',
'extraction.high':'▲ Draws more energy from each rotor turn',
'extraction.low':'▼ Draws little energy from each rotor turn',
'extraction.note':'Higher extraction also brakes the rotor harder, so it needs more steam to hold its speed',
}
pt={
'place':'Valor: %s (%sº de %s)',
'average':'◆ Na média entre as opções',
'moderation':'Moderação',
'moderation.high':'▲ Suaviza a radiação, e as próximas barras de combustível absorvem mais',
'moderation.low':'▼ Quase não suaviza a radiação; a radiação dura passa pelas barras',
'absorption':'Absorção',
'absorption.pro':'+ Converte mais da radiação que passa em calor na carcaça',
'absorption.con':'− Sobra menos radiação para as próximas barras de combustível',
'efficiency':'Eficiência térmica',
'efficiency.high':'▲ Converte quase toda a radiação absorvida em calor',
'efficiency.low':'▼ Desperdiça boa parte da radiação absorvida',
'conductivity':'Condutividade',
'conductivity.high':'▲ Leva o calor das barras de combustível para a carcaça rapidamente',
'conductivity.low':'▼ Conduz o calor devagar; as barras de combustível esquentam mais',
'coil_efficiency':'Eficiência',
'coil_efficiency.high':'▲ Converte mais do trabalho do rotor em FE',
'coil_efficiency.low':'▼ Perde mais do trabalho do rotor',
'bonus':'Bônus',
'bonus.high':'▲ Aumenta o expoente da geração; o ganho cresce com a rotação',
'bonus.low':'▼ Pouco ou nenhum bônus de geração',
'extraction':'Extração',
'extraction.high':'▲ Tira mais energia de cada volta do rotor',
'extraction.low':'▼ Tira pouca energia de cada volta do rotor',
'extraction.note':'Mais extração também freia mais o rotor, que precisa de mais vapor para manter a rotação',
}
old=['turbine_coil_bonus','turbine_coil_efficiency','turbine_coil_extraction','reactor_moderator_moderation','reactor_moderator_absorption','reactor_moderator_conductivity','reactor_moderator_efficiency']
for name,data in (('en_us',en),('pt_br',pt)):
    p='src/main/resources/assets/biggerreactorsrework/lang/%s.json'%name
    s=open(p,encoding='utf-8',newline='').read()
    nl='\r\n' if '\r\n' in s else '\n'
    lines=s.split(nl)
    lines=[l for l in lines if not any('"jei.biggerreactorsrework.classic.%s"'%o in l for o in old)]
    idx=next(i for i,l in enumerate(lines) if '"jei.biggerreactorsrework.classic.reactor_moderator_fluid"' in l)
    new=['  %s: %s,'%(json.dumps(K+k),json.dumps(v,ensure_ascii=False)) for k,v in data.items()]
    lines[idx+1:idx+1]=new
    out=nl.join(lines)
    json.loads(out)
    open(p,'w',encoding='utf-8',newline='').write(out)
print('ok')
