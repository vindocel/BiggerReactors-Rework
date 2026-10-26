"""Read-only NBT inspection of this mod's saved machines for the cooling investigation."""
import io,json,struct,zlib,gzip,pathlib,sys

def parse(raw):
    f=io.BytesIO(raw)
    def read(fmt):return struct.unpack('>'+fmt,f.read(struct.calcsize('>'+fmt)))[0]
    def string():return f.read(read('H')).decode('utf-8',errors='replace')
    def tag(t):
        if t in (1,2,3,4,5,6):return read({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==7:return list(f.read(read('i')))
        if t==8:return string()
        if t==9:
            subtype=read('B');return [tag(subtype) for _ in range(read('i'))]
        if t==10:
            obj={}
            while (kind:=read('B')):
                name=string();obj[name]=tag(kind)
            return obj
        if t in (11,12):return [read('i' if t==11 else 'q') for _ in range(read('i'))]
        raise ValueError(t)
    kind=read('B');string();return tag(kind)

world=pathlib.Path(sys.argv[1] if len(sys.argv)>1 else 'run/saves/Mundo de teste')
metadata=parse(gzip.decompress((world/'level.dat').read_bytes())).get('Data',{})
player=metadata.get('Player',{}).get('Pos',[0,0,0]);px,pz=int(player[0]),int(player[2])
print('WORLD',metadata.get('LevelName'), 'player',player)
machines=[];blocks=[]
for region in (world/'region').glob('*.mca'):
    _,rx,rz,_=region.name.split('.');rx=int(rx);rz=int(rz)
    if not ((px-48)//512<=rx<=(px+48)//512 and (pz-48)//512<=rz<=(pz+48)//512):continue
    raw=region.read_bytes()
    for i in range(1024):
        if not ((px-48)//16<=rx*32+i%32<=(px+48)//16 and (pz-48)//16<=rz*32+i//32<=(pz+48)//16):continue
        offset=int.from_bytes(raw[i*4:i*4+3],'big')*4096
        if not offset:continue
        length=int.from_bytes(raw[offset:offset+4],'big');compression=raw[offset+4]
        packed=raw[offset+5:offset+4+length]
        chunk=parse(zlib.decompress(packed) if compression==2 else gzip.decompress(packed) if compression==1 else packed)
        for be in chunk.get('block_entities',[]):
            if be.get('id','').startswith('biggerreactorsrework:'):machines.append(be)
        cx,cz=chunk.get('xPos',0),chunk.get('zPos',0)
        for section in chunk.get('sections',[]):
            sy=section.get('Y',0);data=section.get('block_states',{});palette=data.get('palette',[])
            relevant={j for j,p in enumerate(palette) if p.get('Name','').startswith('biggerreactorsrework:')}
            if not relevant:continue
            bits=max(4,(len(palette)-1).bit_length());per=64//bits;mask=(1<<bits)-1;longs=data.get('data',[])
            for n in range(4096):
                if not (px-48<=cx*16+n%16<=px+48 and player[1]-32<=sy*16+n//256<=player[1]+48 and pz-48<=cz*16+(n//16)%16<=pz+48):continue
                j=((longs[n//per] & ((1<<64)-1)) >> ((n%per)*bits)) & mask if longs else 0
                if j in relevant:blocks.append({'pos':[cx*16+n%16,sy*16+n//256,cz*16+(n//16)%16],**palette[j]})
pathlib.Path('output/cooling-study/world-machines.json').write_text(json.dumps(machines,indent=2),encoding='utf-8')
pathlib.Path('output/cooling-study/world-blocks.json').write_text(json.dumps(blocks,indent=2),encoding='utf-8')
for be in machines:
    if 'terminal' in be['id']:print(be['id'],[be.get(k) for k in ('x','y','z')])
    data=be.get('sub',{}).get('persistent_multiblock',{}).get('controller_data')
    if data:print('CONTROLLER',be['id'],[be.get(k) for k in ('x','y','z')],json.dumps(data))
