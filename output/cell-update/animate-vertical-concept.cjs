const fs=require('fs'),path=require('path');
const sharp=require('C:/Users/victo/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const SOURCE='C:/Users/victo/Desktop/textures/textures/cell/concepts/vertical-bridge/body-panel-48x32.png';
const OUT=path.join(__dirname,'vertical-charge-16x16');
const put=(im,x,y,c)=>im.data.set(c,(y*im.w+x)*4);
const get=(im,x,y)=>[...im.data.subarray((y*im.w+x)*4,(y*im.w+x)*4+4)];
const blank=(w,h)=>({w,h,data:Buffer.alloc(w*h*4)});
async function save(name,im,scale=1){const p=path.join(OUT,name);fs.mkdirSync(path.dirname(p),{recursive:true});await sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).resize(im.w*scale,im.h*scale,{kernel:'nearest'}).png().toFile(p);}
function compose(b,e){const out={w:b.w,h:b.h,data:Buffer.from(b.data)};for(let i=0;i<e.data.length;i+=4)if(e.data[i+3])e.data.copy(out.data,i,i,i+4);return out;}
(async()=>{
  fs.mkdirSync(OUT,{recursive:true});
  const raw=await sharp(SOURCE).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const body={w:raw.info.width,h:raw.info.height,data:raw.data};
  if(body.w!==48||body.h!==32)throw Error('Expected a 3x2 panel of 16x16 cells');
  const bars=[6,8,10,12,14,16,18,20,22,24,26];
  const colors=[[166,235,250],[55,211,243],[53,202,232],[34,122,171]];
  function energy(level){const im=blank(48,32);for(let c=0;c<3;c++)for(let i=0;i<bars.length;i++)for(let x=6;x<10;x++){
    const lit=bars.length-1-i<level;
    put(im,c*16+x,bars[i],[...(lit?colors[x-6]:[15,43,58]),255]);
  }return im;}
  const levels=[0,1,2,3,4,5,6,7,8,9,10,11,11,11];
  const full=energy(11),preview=compose(body,full),sheet=blank(48,32*levels.length),gif=[];
  await save('body-panel-48x32.png',body);await save('energy-panel-48x32.png',full);
  await save('panel-48x32.png',preview);await save('preview.png',preview,16);
  const sheets=new Map();
  for(let row=0;row<2;row++)for(let col=0;col<3;col++){
    const im=blank(16,16);
    for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(im,x,y,get(body,col*16+x,row*16+y));
    const name=`tiles/${row===0?'top':'bottom'}_${col}`;
    await save(name+'_body.png',im);sheets.set(name,blank(16,16*levels.length));
  }
  for(let frame=0;frame<levels.length;frame++){
    const e=energy(levels[frame]);
    for(let y=0;y<32;y++)for(let x=0;x<48;x++)put(sheet,x,y+frame*32,get(e,x,y));
    for(const [name,im]of sheets){const row=name.includes('/top')?0:1,col=Number(name.at(-1));
      for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(im,x,y+frame*16,get(e,col*16+x,row*16+y));
    }
    const composed=compose(body,e);
    gif.push(await sharp(composed.data,{raw:{width:48,height:32,channels:4}}).resize(768,512,{kernel:'nearest'}).raw().toBuffer());
    // Verify the bright charge frontier advances upward through the cell seam.
    for(let i=0;i<bars.length;i++){
      const lit=get(e,6,bars[i])[0]>100;
      if(lit!==(bars.length-1-i<levels[frame]))throw Error('Incorrect bottom-to-top phase');
    }
  }
  await save('energy-panel-animated.png',sheet);
  const meta={animation:{width:16,height:16,frametime:4,interpolate:false,frames:levels.map((_,i)=>i)}};
  for(const [name,im]of sheets){await save(name+'_energy_animated.png',im);fs.writeFileSync(path.join(OUT,name+'_energy_animated.png.mcmeta'),JSON.stringify(meta,null,2)+'\n');}
  fs.writeFileSync(path.join(OUT,'energy-panel-animated.png.mcmeta'),JSON.stringify({animation:{...meta.animation,width:48,height:32}},null,2)+'\n');
  await sharp(Buffer.concat(gif),{raw:{width:768,height:512*levels.length,channels:4,pageHeight:512}}).gif({delay:levels.map(()=>200),loop:0}).toFile(path.join(OUT,'preview-animated.gif'));
  fs.writeFileSync(path.join(OUT,'LEIA-ME.txt'),'CONCEITO ANIMADO EM PIXELS REAIS\n\nCada bloco mede 16x16. O painel de 3 colunas e 2 blocos de altura mede 48x32. A prévia amplia os pixels 16 vezes, sem suavização.\n\nO fundo escuro e a arte vermelha são os mesmos do conceito aprovado. Onze barras com espaçamento regular percorrem cada coluna, incluindo o encontro entre os dois blocos, como na última imagem de referência.\n\nA animação tem 14 quadros: preenchimento cumulativo da barra inferior até a superior, pausa completa e reinício. Não há trecho descendo. Os seis arquivos de energia em tiles/ têm quadros 16x16, todos sincronizados no mesmo ciclo de 4 ticks por quadro. A fase de cima continua a de baixo; não são duas animações locais repetidas.\n\nAs camadas do corpo e da energia ficam separadas. Os arquivos em tiles/ correspondem às seis posições deste painel de conceito. Para colunas de outras alturas, o renderizador precisa selecionar a fase pela altura real; essa integração não faz parte desta prévia. Nenhuma textura atual ou código do jogo foi substituído.\n');
  fs.writeFileSync(path.join(OUT,'verification.json'),JSON.stringify({panel:[48,32],tile:[16,16],frames:14,ticks_per_frame:4,bars_per_column:11,direction:'bottom_to_top',body_pixels_unchanged:true,pause_frames:3,levels},null,2)+'\n');
  console.log(OUT);
})().catch(e=>{console.error(e);process.exit(1)});
