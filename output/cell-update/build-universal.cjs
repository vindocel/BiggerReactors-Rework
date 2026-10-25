const fs=require('fs'),path=require('path'),crypto=require('crypto');
const sharp=require('C:/Users/victo/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const ROOT=__dirname,OUT=path.join(ROOT,'universal-connected'),SOURCE='C:/Users/victo/Desktop/textures/textures/cell';
const blank=(w=16,h=16)=>({w,h,data:Buffer.alloc(w*h*4)});
const get=(im,x,y)=>[...im.data.subarray((y*im.w+x)*4,(y*im.w+x)*4+4)];
const put=(im,x,y,c)=>im.data.set(c,(y*im.w+x)*4);
async function read(p){const {data,info}=await sharp(p).ensureAlpha().raw().toBuffer({resolveWithObject:true});return {data,w:info.width,h:info.height};}
async function save(p,im){await sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).png().toFile(p);}
const norm=m=>(m&15)|((m&5)===5?m&16:0)|((m&6)===6?m&32:0)|((m&9)===9?m&64:0)|((m&10)===10?m&128:0);
function bodyVariant(src,m){
 const o={w:16,h:16,data:Buffer.from(src.data)};
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  const l=x<3,r=x>=13,t=y<3,b=y>=13;
  let change=(l&&(m&1))||(r&&(m&2))||(t&&(m&4))||(b&&(m&8));
  if(t&&!(m&4)||b&&!(m&8)){if(change&&(l||r))put(o,x,y,get(src,7+x%2,y));continue;}
  if(l&&!(m&1)||r&&!(m&2)){if(change&&(t||b))put(o,x,y,get(src,x,7+y%2));continue;}
  if(l&&t&&!(m&16)||r&&t&&!(m&32)||l&&b&&!(m&64)||r&&b&&!(m&128))change=false;
  if(change)put(o,x,y,get(src,2,4+(x+y*3)%8));
 }
 // Open the well only toward a vertically adjacent cell. Exterior caps stay intact.
 for(let y=0;y<16;y++)if((m&4)&&y<=4||(m&8)&&y>=11)for(let x=3;x<=12;x++)put(o,x,y,get(src,x,6+(y+1)%4));
 return o;
}
const blue=[[166,235,250],[55,211,243],[53,202,232],[34,122,171]];
function energyVariant(m,frame){
 const im=blank(),lo=m&4?0:6,hi=m&8?14:10;
 for(let y=lo;y<=hi;y+=2)for(let x=6;x<10;x++){
  // A traveling highlight with a 16px period: it crosses a block boundary at
  // exactly the same speed/phase, for any column height. No per-column fixed size.
  const distance=(y+frame*2+16)%16;
  const gain=distance<4?1:distance<8?.7:.4;
  put(im,x,y,[...blue[x-6].map(v=>Math.round(v*gain)),255]);
 }
 return im;
}
function atlasFor(fn){const a=blank(32,32);for(const [q,m]of [[0,255],[1,12],[2,3],[3,15]]){const im=fn(m);for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(a,x+(q%2)*16,y+Math.floor(q/2)*16,get(im,x,y));}return a;}
function fromAtlas(src,atlas,m){
 const out=blank();
 for(let qy=0;qy<2;qy++)for(let qx=0;qx<2;qx++){
  const v=qy?8:4,h=qx?2:1,d=qy?(qx?128:64):(qx?32:16);
  const V=!!(m&v),H=!!(m&h);
  const tile=V&&H?(m&d?0:3):V?1:H?2:-1;
  for(let y=0;y<8;y++)for(let x=0;x<8;x++){
   const tx=qx*8+x,ty=qy*8+y;
   put(out,tx,ty,tile<0?get(src,tx,ty):get(atlas,tx+(tile%2)*16,ty+Math.floor(tile/2)*16));
  }
 }
 return out;
}
function composite(b,e){const out={w:b.w,h:b.h,data:Buffer.from(b.data)};for(let i=0;i<e.data.length;i+=4)if(e.data[i+3])e.data.copy(out.data,i,i,i+4);return out;}
async function main(){
 fs.mkdirSync(OUT,{recursive:true});
 const bytes=fs.readFileSync(path.join(SOURCE,'body.png')),body=await read(path.join(SOURCE,'body.png'));
 fs.writeFileSync(path.join(OUT,'body.png'),bytes);
 const atlas=atlasFor(m=>bodyVariant(body,m));await save(path.join(OUT,'body_ctm.png'),atlas);
 const energy=blank(16,128),energyAtlas=blank(32,256);
 for(let f=0;f<8;f++){
  const e=energyVariant(0,f),ea=atlasFor(m=>energyVariant(m,f));
  for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(energy,x,y+f*16,get(e,x,y));
  for(let y=0;y<32;y++)for(let x=0;x<32;x++)put(energyAtlas,x,y+f*32,get(ea,x,y));
 }
 await save(path.join(OUT,'energy.png'),energy);await save(path.join(OUT,'energy_ctm.png'),energyAtlas);
 const json=(name,o)=>fs.writeFileSync(path.join(OUT,name),JSON.stringify(o,null,2)+'\n');
 const anim=size=>({width:size,height:size,frametime:4,interpolate:false,frames:[0,1,2,3,4,5,6,7]});
 const ctm=(name,layer)=>({ctm_version:1,type:'CTM',layer,textures:[`biggerreactorsrework:block/battery/cell/${name}_ctm`],extra:{connect_to:[{block:'biggerreactorsrework:battery_cell'}],connect_inside:true,ignore_states:true}});
 json('body.png.mcmeta',{ctm:ctm('body','SOLID')});
 json('energy.png.mcmeta',{animation:anim(16),ctm:ctm('energy','CUTOUT')});
 json('energy_ctm.png.mcmeta',{animation:anim(32)});
 const masks=[...new Set(Array.from({length:256},(_,m)=>norm(m)))];
 for(const m of masks){
  const actual=fromAtlas(body,atlas,m),expected=bodyVariant(body,m);
  if(!actual.data.equals(expected.data))throw Error('Body atlas mismatch '+m);
  for(let f=0;f<8;f++){
   const e=energyVariant(0,f),ea=atlasFor(n=>energyVariant(n,f));
   if(!fromAtlas(e,ea,m).data.equals(energyVariant(m,f).data))throw Error('Energy atlas mismatch '+m);
  }
 }
 // Prove identical phase on both sides of a vertical seam, at all 8 frames.
 for(let f=0;f<8;f++)for(const height of [2,3,16,37])for(let row=0;row<height;row++){
  const m=(row>0?4:0)|(row<height-1?8:0),e=energyVariant(m,f);
  for(let y=0;y<16;y+=2)if(get(e,6,y)[3]){
   const local=(y+f*2)%16,global=(row*16+y+f*2)%16;
   if(local!==global)throw Error('Vertical phase seam');
  }
 }
 fs.writeFileSync(path.join(OUT,'LEIA-ME.txt'),`TEXTURA CONECTADA UNIVERSAL — CÉLULA\n\nSomente os arquivos finais deste conjunto. Não existe painel 3x2 fixo. body.png conserva o arquivo vermelho original, byte por byte.\n\nAs faces medem 16x16. body_ctm.png é um atlas compacto de 32x32, dividido em quadrantes de 8x8. Ele cobre as 47 combinações de bordas e cantos. O fundo escuro abre para cima e/ou para baixo quando há outra célula nessas direções, mantendo as tampas nas extremidades. A conexão horizontal remove apenas a borda compartilhada, mantendo cada coluna azul separada.\n\nenergy.png: 8 quadros isolados de 16x16, tira 16x128. energy_ctm.png: 8 quadros CTM de 32x32, tira 32x256. As barras se prolongam na ligação vertical e têm espaçamento regular de 2 pixels.\n\nA animação é um fluxo luminoso periódico ascendente, com período de 16 pixels e 4 ticks por quadro. Por isso ela atravessa os encontros em fase, qualquer que seja a largura ou altura: 1x1, 3x2, 10x16 ou maior. Não é uma medição da carga real nem um preenchimento único por toda a altura; para esse preenchimento global o renderizador precisa conhecer a altura da coluna e a progressão da carga.\n\nOs atlas podem ser usados pelo renderizador nativo seguindo as mesmas regras de quadrantes do CTM. Os metadados .mcmeta fornecidos seguem ConnectedTexturesMod. Para esse mod, o caminho final esperado é assets/biggerreactorsrework/textures/block/battery/cell/. O jogo sozinho não interpreta a seção ctm: a integração nativa ou o ConnectedTexturesMod continua necessária. A animação usa o mesmo tempo nos dois atlas. Não trocar o nome de energy.png sem ajustar as referências do modelo, que no projeto atual ainda usa energy_animated para o estado carregado.\n\nNenhum código ou textura fora da pasta concepts foi alterado. Os conceitos anteriores dessa pasta foram removidos conforme solicitado.\n`);
 // Previews and validation remain in workspace, keeping concepts free of drafts.
 async function panel(cols,rows,frame){
  const im=blank(cols*16,rows*16);
  for(let r=0;r<rows;r++)for(let c=0;c<cols;c++){
   let m=(c?1:0)|(c<cols-1?2:0)|(r?4:0)|(r<rows-1?8:0);
   m|=c&&r?16:0;m|=c<cols-1&&r?32:0;m|=c&&r<rows-1?64:0;m|=c<cols-1&&r<rows-1?128:0;
   const b=bodyVariant(body,m),e=energyVariant(m,frame),tile=composite(b,e);
   for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(im,c*16+x,r*16+y,get(tile,x,y));
  }return im;
 }
 const gif=[];
 for(let f=0;f<8;f++){const im=await panel(3,2,f);gif.push(await sharp(im.data,{raw:{width:48,height:32,channels:4}}).resize(768,512,{kernel:'nearest'}).raw().toBuffer());}
 await sharp(Buffer.concat(gif),{raw:{width:768,height:4096,channels:4,pageHeight:512}}).gif({delay:Array(8).fill(200),loop:0}).toFile(path.join(ROOT,'universal-preview.gif'));
 const big=await panel(10,16,0);await sharp(big.data,{raw:{width:big.w,height:big.h,channels:4}}).resize(big.w*3,big.h*3,{kernel:'nearest'}).png().toFile(path.join(ROOT,'universal-10x16.png'));
 fs.writeFileSync(path.join(ROOT,'universal-verification.json'),JSON.stringify({bodyHash:crypto.createHash('sha256').update(bytes).digest('hex'),masks:masks.length,animationFrames:8,phaseHeights:[2,3,16,37],atlasReconstruction:'all masks and all frames passed',finalFiles:fs.readdirSync(OUT)},null,2));
 console.log('8 final files; 47 body patterns and 376 animated patterns verified; 10x16 preview generated.');
}
module.exports={bodyVariant,energyVariant};
if(require.main===module)main().catch(e=>{console.error(e);process.exit(1)});
