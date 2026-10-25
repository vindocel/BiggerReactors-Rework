const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/victo/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const SOURCE='C:/Users/victo/Desktop/textures/textures/cell';
const OUT=path.join(__dirname,'vertical-concept');
async function read(p){const {data,info}=await sharp(p).ensureAlpha().raw().toBuffer({resolveWithObject:true});return {data,w:info.width,h:info.height};}
const blank=()=>({w:48,h:32,data:Buffer.alloc(48*32*4)});
const get=(im,x,y)=>[...im.data.subarray((y*im.w+x)*4,(y*im.w+x)*4+4)];
const put=(im,x,y,p)=>im.data.set(p,(y*im.w+x)*4);
async function save(name,im,scale=1){await sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).resize(im.w*scale,im.h*scale,{kernel:'nearest'}).png().toFile(path.join(OUT,name));}
(async()=>{
  fs.mkdirSync(OUT,{recursive:true});
  const original=await read(path.join(SOURCE,'body.png')),energy=await read(path.join(SOURCE,'energy.png'));
  const body=blank(),glow=blank();
  for(let row=0;row<2;row++)for(let col=0;col<3;col++){
    let mask=(col>0?1:0)|(col<2?2:0)|(row>0?4:0)|(row<1?8:0);
    mask|=(col>0&&row>0?16:0)|(col<2&&row>0?32:0)|(col>0&&row<1?64:0)|(col<2&&row<1?128:0);
    const tile=await read(path.join(SOURCE,'connected',`body_${mask.toString(16).padStart(2,'0')}.png`));
    for(let y=0;y<16;y++)for(let x=0;x<16;x++){
      put(body,col*16+x,row*16+y,get(tile,x,y));
      put(glow,col*16+x,row*16+y,get(energy,x,y));
    }
  }
  for(let col=0;col<3;col++){
    // Connect only the dark recessed well and its thin side bevels.
    // Every pixel outside these three bridge rectangles is left unchanged.
    for(let y=11;y<=20;y++)for(let x=3;x<=12;x++){
      put(body,col*16+x,y,get(original,x,6+((y+1)%4)));
    }
    for(const y of [14,16,18])for(let x=6;x<10;x++)put(glow,col*16+x,y,get(energy,x,8));
  }
  const composed={w:48,h:32,data:Buffer.from(body.data)};
  for(let i=0;i<glow.data.length;i+=4)if(glow.data[i+3])glow.data.copy(composed.data,i,i,i+4);
  await save('body-panel-48x32.png',body);await save('energy-panel-48x32.png',glow);
  await save('panel-48x32.png',composed);await save('preview.png',composed,16);
  // All bars fit the original four-pixel energy aperture in each cell column.
  for(let col=0;col<3;col++){
    let lit=0;for(let y=0;y<32;y++)if(get(glow,col*16+6,y)[3])lit++;
    if(lit!==9)throw Error('Expected nine bars per column');
  }
  fs.writeFileSync(path.join(OUT,'LEIA-ME.txt'),'CONCEITO — CONEXÃO VERTICAL\n\nPainel de três colunas e duas células de altura, em 48x32 pixels reais. Cada coluna tem fundo escuro contínuo e nove barrinhas: três em cima, três novas na ponte central e três embaixo. preview.png é apenas a ampliação 16x sem suavização. As camadas do corpo e da energia estão separadas.\n\nA textura granulada do usuário foi preservada fora dos retângulos da ponte central. Nenhum arquivo de textura atual foi substituído. Este estudo não altera o atlas CTM, as variantes ou o comportamento no jogo; mostra a direção visual proposta.\n');
  console.log(OUT);
})().catch(e=>{console.error(e);process.exit(1)});
