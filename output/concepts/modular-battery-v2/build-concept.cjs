const fs = require('node:fs');
const path = require('node:path');
const sharp = require('C:/Users/victo/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const OUT = __dirname;
const ROOT = path.resolve(OUT, '../../..');
const SOURCE = path.join(ROOT, 'src/main/resources/assets/biggerreactorsrework/textures/block');
const GENERATED = 'C:/Users/victo/.codex/generated_images/01a1713e-e855-7f50-85e2-f89ad2db5314/exec-4cade184-f701-4c35-b52f-60f624364421.png';
const assets = new Map();
const clamp = n => Math.max(0, Math.min(255, Math.round(n)));
async function read(p) {
  const {data, info} = await sharp(p).ensureAlpha().raw().toBuffer({resolveWithObject: true});
  return {data, w:info.width, h:info.height};
}
async function save(name, im) {
  const target = path.join(OUT, 'textures', name + '.png');
  fs.mkdirSync(path.dirname(target), {recursive:true});
  await sharp(im.data, {raw:{width:im.w,height:im.h,channels:4}}).png().toFile(target);
  assets.set(name, im);
}
// Constrain the generated palette direction to the ORIGINAL source pixel layout.
// Each source shade maps to one output shade: there is no spatial redrawing.
const stops = [[0,[0,0,0]],[31,[77,77,72]],[47,[155,153,143]],[57,[186,183,171]],
  [70,[204,201,189]],[88,[225,222,210]],[130,[245,242,232]],[255,[255,255,249]]];
function cream(v) {
  for(let i=1;i<stops.length;i++) if(v<=stops[i][0]) {
    const [a,A]=stops[i-1], [b,B]=stops[i], t=(v-a)/(b-a);
    return A.map((n,j)=>clamp(n+(B[j]-n)*t));
  }
  return stops.at(-1)[1];
}
function recolor(src, kind) {
  const im = {data:Buffer.from(src.data), w:src.w, h:src.h};
  for(let y=0;y<im.h;y++) for(let x=0;x<im.w;x++) {
    const i=(y*im.w+x)*4, [r,g,b,a]=src.data.subarray(i,i+4), localY=y%16;
    if(!a) continue;
    const gray=Math.max(r,g,b)-Math.min(r,g,b)<7;
    const screen=kind==='terminal' && x>=2 && x<=13 && localY>=2 && localY<=11;
    const portCenter=kind==='port' && x>=4 && x<=11 && localY>=4 && localY<=11;
    let rgb=[r,g,b];
    if(gray && !screen && !portCenter) rgb=cream((r+g+b)/3);
    if(kind==='glass' && r>120 && g>120 && b<90) rgb=[220,230,232];
    if((kind==='terminal'||kind==='port') && (g>r*1.25 || b>r*1.25) && Math.max(g,b)>35) {
      const level=Math.max(g,b)/255;
      rgb=[clamp(55*level),clamp(211*level),clamp(243*level)];
    }
    im.data.set(rgb,i);
  }
  return im;
}
function svg(text,w,h) {return Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">${text}</svg>`);}
const text=(x,y,t,size=20,color='#e7e5dd')=>`<text x="${x}" y="${y}" fill="${color}" font-family="Segoe UI,Arial" font-size="${size}">${t}</text>`;
async function tile(im, size) {return sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).extract({left:0,top:0,width:16,height:16}).resize(size,size,{kernel:'nearest'}).png().toBuffer();}
async function board(filename,w,h,labels,items) {
  const overlays=[];
  for(const {im,x,y,size} of items) overlays.push({input:await tile(im,size),left:x,top:y});
  overlays.push({input:svg(labels,w,h),left:0,top:0});
  await sharp({create:{width:w,height:h,channels:4,background:'#24272b'}}).composite(overlays).png().toFile(path.join(OUT,filename));
}
function blank(w=16,h=16) {return {w,h,data:Buffer.alloc(w*h*4)};}
function pixel(im,x,y,rgba) {im.data.set(rgba,(y*im.w+x)*4);}
function sample(im,x,y) {return [...im.data.subarray((y*im.w+x)*4,(y*im.w+x)*4+4)];}
async function main() {
  const casing=['corner','frame','face','disassembled'];
  for(const n of casing) await save('casing/'+n,recolor(await read(path.join(SOURCE,'reactor/casing',n+'.png')),'casing'));
  for(const n of fs.readdirSync(path.join(SOURCE,'reactor/glass')).filter(n=>n.endsWith('.png'))) {
    await save('glass/'+n.slice(0,-4),recolor(await read(path.join(SOURCE,'reactor/glass',n)),'glass'));
  }
  for(const n of ['off','idle','active']) {
    await save('terminal/'+n,recolor(await read(path.join(SOURCE,'reactor/terminal',n+'.png')),'terminal'));
    const meta=path.join(SOURCE,'reactor/terminal',n+'.png.mcmeta');
    if(fs.existsSync(meta)) fs.copyFileSync(meta,path.join(OUT,'textures/terminal',n+'.png.mcmeta'));
  }
  for(const n of ['inlet','outlet']) await save('energy_port/'+n,recolor(await read(path.join(SOURCE,'reactor/access_port',n+'.png')),'port'));
  // Finalize generated cell to a strict 16x16 grid and small hand-selected palette.
  const generated=await read(GENERATED), combined=blank(), body=blank(), energy=blank();
  const redPalette=[[39,24,28],[65,40,45],[92,48,51],[112,57,58],[135,70,71],[167,93,88]];
  const bluePalette=[[21,75,106],[34,122,171],[53,202,232],[55,211,243],[166,235,250]];
  function nearest(rgb,palette) {return palette.reduce((best,p)=>rgb.reduce((s,v,j)=>s+(v-p[j])**2,0)<rgb.reduce((s,v,j)=>s+(v-best[j])**2,0)?p:best);}
  for(let y=0;y<16;y++) for(let x=0;x<16;x++) {
    const rgb=sample(generated,Math.floor((x+.5)*generated.w/16),Math.floor((y+.5)*generated.h/16)).slice(0,3);
    const isEnergy=rgb[2]>rgb[0]*1.4 && rgb[1]>rgb[0]*1.3;
    const color=nearest(rgb,isEnergy?bluePalette:redPalette);
    pixel(combined,x,y,[...color,255]);
    pixel(body,x,y,[...(isEnergy?[39,24,28]:color),255]);
    if(isEnergy) pixel(energy,x,y,[...color,255]);
  }
  await save('cell/body',body); await save('cell/energy',energy); await save('cell/preview',combined);
  const anim=blank(16,64), previewAnim=blank(16,64);
  for(let f=0;f<4;f++) for(let y=0;y<16;y++) for(let x=0;x<16;x++) {
    const e=sample(energy,x,y), base=sample(body,x,y);
    if(e[3]) {
      // Traveling blue highlight, same fixed shape; frame zero matches static preview.
      let rgb=e.slice(0,3);
      if(f>0) rgb=bluePalette[(x+y+f*2)%7<2?4:((x+y+f)%3)+1];
      pixel(anim,x,y+f*16,[...rgb,255]);
      pixel(previewAnim,x,y+f*16,[...rgb,255]);
    } else pixel(previewAnim,x,y+f*16,base);
  }
  await save('cell/energy_animated',anim);
  fs.writeFileSync(path.join(OUT,'textures/cell/energy_animated.png.mcmeta'),JSON.stringify({animation:{width:16,height:16,frametime:4,interpolate:false,frames:[0,1,2,3]}},null,2)+'\n');
  // Front, top, and sides can share this simple cube texture at concept stage.
  const W=1152,H=840;
  let labels=text(32,42,'BATERIA · CONCEITO 02 · PNGs reais de 16 × 16',28);
  labels+=text(32,74,'Mesmo desenho dos blocos originais; mudança principal: paleta creme.',18,'#bcbcb4');
  const cols=[270,475,680,885], names=['Canto','Aresta','Face','Desmontada'];
  names.forEach((n,i)=>labels+=text(cols[i],115,n,19));
  const rows=[['Reator original','reactor'],['Turbina original','turbine'],['Bateria creme',null]],items=[];
  for(let r=0;r<3;r++) {
    const y=140+r*210;
    labels+=text(32,y+72,rows[r][0],21,r===2?'#eee4ca':'#bcbcb4');
    for(let c=0;c<4;c++) {
      const im=rows[r][1]?await read(path.join(SOURCE,rows[r][1],'casing',casing[c]+'.png')):assets.get('casing/'+casing[c]);
      items.push({im,x:cols[c],y,size:160});
    }
  }
  labels+=text(32,805,'Ampliação 10× sem suavização · desenhos e posições dos pixels preservados.',18,'#bcbcb4');
  await board('01-comparacao-casing.png',W,H,labels,items);
  const groups=[['glass/connected_none','Vidro isolado'],['energy_port/inlet','Entrada'],['energy_port/outlet','Saída'],['terminal/off','Terminal desligado'],['terminal/idle','Terminal em espera'],['terminal/active','Terminal ativo'],['cell/body','Célula: corpo'],['cell/energy','Célula: energia'],['cell/preview','Célula: composição']];
  let lab=text(32,40,'BATERIA · COMPONENTES 16 × 16',28); const its=[];
  for(let i=0;i<groups.length;i++) {
    const x=36+(i%3)*300,y=85+Math.floor(i/3)*245;
    its.push({im:assets.get(groups[i][0]),x:x+40,y,size:160});
    lab+=text(x,y+192,groups[i][1],21);
  }
  lab+=text(32,830,'Energia azul extraída da paleta da barra do mod · camadas separadas.',17,'#bcbcb4');
  await board('02-componentes.png',936,860,lab,its);
  // Connected 2x2 pane: union of existing edge masks, original geometry retained.
  const window=blank(32,32);
  const glassNames=['connected_br','connected_bl','connected_tr','connected_tl'];
  for(let q=0;q<4;q++) {
    const im=assets.get('glass/'+glassNames[q]);
    for(let y=0;y<16;y++) for(let x=0;x<16;x++) pixel(window,x+(q%2)*16,y+Math.floor(q/2)*16,sample(im,x,y));
  }
  await sharp(window.data,{raw:{width:32,height:32,channels:4}}).resize(384,384,{kernel:'nearest'}).png().toFile(path.join(OUT,'03-vidro-conectado.png'));
  await renderAssembly();
  // Small GIF previews use lossless nearest-neighbor enlarged sprite frames.
  const gifRaw=Buffer.alloc(128*128*4*4);
  for(let f=0;f<4;f++) {
    const frame=await sharp(previewAnim.data,{raw:{width:16,height:64,channels:4}}).extract({left:0,top:f*16,width:16,height:16}).resize(128,128,{kernel:'nearest'}).raw().toBuffer();
    frame.copy(gifRaw,f*128*128*4);
  }
  await sharp(gifRaw,{raw:{width:128,height:512,channels:4,pageHeight:128}}).gif({delay:[200,200,200,200],loop:0}).toFile(path.join(OUT,'05-celula-animada.gif'));
  // Validate actual file dimensions, alpha preservation and casing topology.
  const verification=[];
  for(const [name,im] of assets) {
    if(im.w!==16 || im.h%16!==0) throw Error('Invalid pixel dimensions: '+name);
    verification.push({file:'textures/'+name+'.png',width:im.w,height:im.h,frames:im.h/16});
  }
  for(const n of casing) {
    const src=await read(path.join(SOURCE,'reactor/casing',n+'.png')),dst=assets.get('casing/'+n),m=new Map();
    for(let i=0;i<src.data.length;i+=4) {
      const key=src.data.subarray(i,i+4).toString('hex'), val=dst.data.subarray(i,i+4).toString('hex');
      if(m.has(key)&&m.get(key)!==val) throw Error('Source layout changed'); m.set(key,val);
    }
  }
  for(const [name,im] of assets) if(name.startsWith('glass/')) {
    const src=await read(path.join(SOURCE,'reactor',name+'.png'));
    for(let i=3;i<im.data.length;i+=4) if(src.data[i]!==im.data[i]) throw Error('Glass alpha changed');
  }
  fs.writeFileSync(path.join(OUT,'verification.json'),JSON.stringify({casing:'source shade mapping only; original pixel geometry preserved',glass:'source alpha preserved in every pixel',assets:verification},null,2)+'\n');
  console.log(`Saved ${assets.size} textures; validated dimensions, casing source geometry and glass alpha.`);
}

// Small software rasterizer: actual supplied 16x16 sprites, nearest-neighbor sampling.
// Geometry is exactly a 3x3x3 voxel shell and one central 1x1x1 cell.
async function renderAssembly() {
  const w=144,h=160;
  const project=([x,y,z])=>[72+(x-z)*19,82+(x+z)*9.5-y*19];
  function render(cut=false) {
    const canvas=blank(w,h),faces=[],blocks=new Map();
    for(let x=0;x<3;x++) for(let y=0;y<3;y++) for(let z=0;z<3;z++) {
      const edges=[x,y,z].filter(v=>v===0||v===2).length;
      if(edges===0) {blocks.set(`${x},${y},${z}`,{x,y,z,cell:true});continue;}
      if(cut && (x===2||z===2||(y===2&&x>0&&z>0))) continue;
      blocks.set(`${x},${y},${z}`,{x,y,z,edges});
    }
    function tex(b,axis) {
      if(b.cell) return assets.get('cell/preview');
      if(b.edges>=3) return assets.get('casing/corner');
      if(b.edges===2) {
        const src=assets.get('casing/frame');
        // A longitudinal rail follows the edge axis; horizontal runs rotate 90 degrees.
        const railAxis=b.x===1?'x':b.z===1?'z':'y';
        if(railAxis!=='y') {
          const out=blank();
          for(let y=0;y<16;y++) for(let x=0;x<16;x++) pixel(out,x,y,sample(src,y,15-x));
          return out;
        }
        return src;
      }
      if(axis==='z'&&b.x===1&&b.y===1&&b.z===2) return assets.get('terminal/active');
      if((axis==='x'&&b.x===2&&b.y===1&&b.z===1)||(axis==='y'&&b.y===2&&b.x===1&&b.z===1)) return assets.get('glass/connected_none');
      return assets.get('casing/face');
    }
    for(const b of blocks.values()) for(const axis of ['z','x','y']) {
      const {x,y,z}=b,neighbor=axis==='x'?[x+1,y,z]:axis==='y'?[x,y+1,z]:[x,y,z+1];
      const next=blocks.get(neighbor.join(','));
      const nextGlass=next && ((next.x===2&&next.y===1&&next.z===1)||(next.y===2&&next.x===1&&next.z===1));
      if(next&&!nextGlass&&!b.cell) continue;
      const pts=axis==='z'?[[x,y+1,z+1],[x+1,y+1,z+1],[x+1,y,z+1],[x,y,z+1]]:
        axis==='x'?[[x+1,y+1,z+1],[x+1,y+1,z],[x+1,y,z],[x+1,y,z+1]]:
        [[x,y+1,z],[x+1,y+1,z],[x+1,y+1,z+1],[x,y+1,z+1]];
      faces.push({pts:pts.map(project),tex:tex(b,axis),depth:x+y+z,shade:axis==='x'?.8:axis==='y'?1:.94});
    }
    faces.sort((a,b)=>a.depth-b.depth);
    for(const f of faces) {
      const [a,b,,d]=f.pts,ux=b[0]-a[0],uy=b[1]-a[1],vx=d[0]-a[0],vy=d[1]-a[1],det=ux*vy-uy*vx;
      const minX=Math.max(0,Math.floor(Math.min(...f.pts.map(p=>p[0])))),maxX=Math.min(w-1,Math.ceil(Math.max(...f.pts.map(p=>p[0]))));
      const minY=Math.max(0,Math.floor(Math.min(...f.pts.map(p=>p[1])))),maxY=Math.min(h-1,Math.ceil(Math.max(...f.pts.map(p=>p[1]))));
      for(let y=minY;y<=maxY;y++) for(let x=minX;x<=maxX;x++) {
        const dx=x+.5-a[0],dy=y+.5-a[1],u=(dx*vy-dy*vx)/det,v=(ux*dy-uy*dx)/det;
        if(u<0||v<0||u>=1||v>=1) continue;
        const c=sample(f.tex,Math.floor(u*16),Math.floor(v*16));
        if(!c[3]) continue;
        const dest=sample(canvas,x,y),alpha=c[3]/255;
        pixel(canvas,x,y,[...c.slice(0,3).map((n,i)=>clamp(n*f.shade*alpha+dest[i]*(1-alpha))),255]);
      }
    }
    return canvas;
  }
  const full=render(),cut=render(true);
  const overlays=[];
  for(const [i,im] of [full,cut].entries()) overlays.push({input:await sharp(im.data,{raw:{width:w,height:h,channels:4}}).resize(w*4,h*4,{kernel:'nearest'}).png().toBuffer(),left:20+i*580,top:70});
  const labels=text(32,40,'BATERIA 3 × 3 × 3 · AS MESMAS TEXTURAS DE 16 × 16',26)+text(120,720,'Montada: terminal + vidro',23)+text(690,720,'Corte: uma célula interna',23)+text(32,770,'Visualização dos sprites reais, ampliada sem suavização. Estudo de arte; sem integração no jogo.',17,'#bcbcb4');
  overlays.push({input:svg(labels,1180,800),left:0,top:0});
  await sharp({create:{width:1180,height:800,channels:4,background:'#24272b'}}).composite(overlays).png().toFile(path.join(OUT,'04-montagem-3x3x3.png'));
}
main().catch(e=>{console.error(e);process.exit(1)});
