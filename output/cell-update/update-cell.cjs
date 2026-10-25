const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const sharp=require('C:/Users/victo/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const ROOT=__dirname, TARGET='C:/Users/victo/Desktop/textures/textures/cell', STAGE=path.join(ROOT,'ready');
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const blank=(w=16,h=16)=>({w,h,data:Buffer.alloc(w*h*4)});
const get=(im,x,y)=>[...im.data.subarray((y*im.w+x)*4,(y*im.w+x)*4+4)];
const put=(im,x,y,c)=>im.data.set(c,(y*im.w+x)*4);
async function read(p){const {data,info}=await sharp(p).ensureAlpha().raw().toBuffer({resolveWithObject:true});return {w:info.width,h:info.height,data};}
async function save(name,im){const p=path.join(STAGE,name);fs.mkdirSync(path.dirname(p),{recursive:true});await sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).png().toFile(p);}
function json(name,obj){fs.mkdirSync(path.dirname(path.join(STAGE,name)),{recursive:true});fs.writeFileSync(path.join(STAGE,name),JSON.stringify(obj,null,2)+'\n');}
function compose(body,energy){const out={w:body.w,h:body.h,data:Buffer.from(body.data)};for(let i=0;i<out.data.length;i+=4)if(energy.data[i+3])energy.data.copy(out.data,i,i,i+4);return out;}
function norm(m){return(m&15)|((m&5)===5?(m&16):0)|((m&6)===6?(m&32):0)|((m&9)===9?(m&64):0)|((m&10)===10?(m&128):0);}
function connected(body,m){
  const out={w:16,h:16,data:Buffer.from(body.data)};
  for(let y=0;y<16;y++)for(let x=0;x<16;x++){
    const l=x<3,r=x>=13,t=y<3,b=y>=13;
    let change=(l&&(m&1))||(r&&(m&2))||(t&&(m&4))||(b&&(m&8));
    // The visible outline and grain are copied exactly from the user's texture.
    // Only borders adjoining another cell are eligible for replacement.
    if((t&&!(m&4))||(b&&!(m&8))) {
      if(change&&(l||r))put(out,x,y,get(body,7+(x%2),y));
      continue;
    }
    if((l&&!(m&1))||(r&&!(m&2))) {
      if(change&&(t||b))put(out,x,y,get(body,x,7+(y%2)));
      continue;
    }
    if(l&&t&&!(m&16))change=false;
    if(r&&t&&!(m&32))change=false;
    if(l&&b&&!(m&64))change=false;
    if(r&&b&&!(m&128))change=false;
    if(change)put(out,x,y,get(body,2,4+((x+y*3)%8)));
  }
  return out;
}
async function board(name,body,energy,variants){
  const W=1040,H=710,overlays=[];
  const label=(x,y,t,size=20)=>`<text x="${x}" y="${y}" fill="#e2e2de" font-family="Segoe UI,Arial" font-size="${size}">${t}</text>`;
  let labels=label(28,36,'CÉLULA · BARRINHAS AZUIS + CONEXÃO',26);
  async function item(im,x,y,size){overlays.push({input:await sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).resize(size,size,{kernel:'nearest'}).png().toBuffer(),left:x,top:y});}
  await item(body,35,78,192);await item(energy,285,78,192);await item(compose(body,energy),535,78,192);
  labels+=label(35,305,'Seu corpo, intacto')+label(285,305,'Brilho separado')+label(535,305,'Três barras');
  const joined=blank(48,32);
  for(let y=0;y<2;y++)for(let x=0;x<3;x++){
    let m=(x>0?1:0)|(x<2?2:0)|(y>0?4:0)|(y<1?8:0);
    m|=(x>0&&y>0?16:0)|(x<2&&y>0?32:0)|(x>0&&y<1?64:0)|(x<2&&y<1?128:0);
    const im=compose(variants.get(m),energy);
    for(let j=0;j<16;j++)for(let i=0;i<16;i++)put(joined,x*16+i,y*16+j,get(im,i,j));
  }
  overlays.push({input:await sharp(joined.data,{raw:{width:48,height:32,channels:4}}).resize(576,384,{kernel:'nearest'}).png().toBuffer(),left:390,top:315});
  labels+=label(30,420,'Células conectadas:')+label(30,450,'sem bordas internas.')+label(30,495,'47 variações de 16 × 16',18)+label(30,525,'+ atlas CTM de 32 × 32',18)+label(30,575,'Cantos côncavos incluídos.',17);
  overlays.push({input:Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}">${labels}</svg>`),left:0,top:0});
  await sharp({create:{width:W,height:H,channels:4,background:'#24272b'}}).composite(overlays).png().toFile(path.join(STAGE,name));
}
async function main(){
  fs.mkdirSync(STAGE,{recursive:true});
  const originalBody=fs.readFileSync(path.join(TARGET,'body.png')),body=await read(path.join(TARGET,'body.png'));
  const oldEnergy=await read(path.join(TARGET,'energy.png'));
  if(body.w!==16||body.h!==16)throw Error('Expected original 16x16 body');
  const energy=blank(),frames=[];
  // Finalized on the original 16x16 grid; no generated red pixels are used.
  const blue=[[166,235,250],[55,211,243],[53,202,232],[34,122,171]];
  for(const y of [6,8,10])for(let x=6;x<10;x++){
    if(get(oldEnergy,x,y)[3]===0)throw Error('Bars leave original energy aperture');
    put(energy,x,y,[...blue[x-6],255]);
  }
  await save('energy.png',energy);await save('preview.png',compose(body,energy));
  // Bottom-to-top charge pulse: dim -> 1 bar -> 2 -> 3 -> hold -> reset.
  const level=[0,1,2,3,3,3,2,1],anim=blank(16,16*level.length),gifParts=[];
  for(let f=0;f<level.length;f++){
    const frame=blank();
    for(let bar=0;bar<3;bar++)for(let x=6;x<10;x++){
      const y=10-bar*2,on=bar<level[f];
      const color=on?blue[x-6]:[17,49,67];
      put(frame,x,y,[...color,255]);put(anim,x,y+f*16,[...color,255]);
    }
    frames.push(frame);
    const composed=compose(body,frame);
    gifParts.push(await sharp(composed.data,{raw:{width:16,height:16,channels:4}}).resize(192,192,{kernel:'nearest'}).raw().toBuffer());
  }
  await save('energy_animated.png',anim);
  json('energy_animated.png.mcmeta',{animation:{width:16,height:16,frametime:4,interpolate:false,frames:level.map((_,i)=>i)}});
  await sharp(Buffer.concat(gifParts),{raw:{width:192,height:192*frames.length,channels:4,pageHeight:192}}).gif({delay:Array(frames.length).fill(200),loop:0}).toFile(path.join(STAGE,'preview-animated.gif'));
  const masks=[...new Set(Array.from({length:256},(_,m)=>norm(m)))].sort((a,b)=>a-b),variants=new Map();
  for(const mask of masks){const im=connected(body,mask);variants.set(mask,im);await save(`connected/body_${mask.toString(16).padStart(2,'0')}.png`,im);}
  // Standard CTM: 4x4 submaps (8x8 each) packed in a 32x32 atlas.
  // Upper-left fully connected; upper-right vertical; lower-left horizontal;
  // lower-right both adjacent sides present, but diagonal absent (concave).
  const atlas=blank(32,32);
  for(const [q,m] of [[0,255],[1,12],[2,3],[3,15]]){
    const im=variants.get(norm(m));
    for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(atlas,x+(q%2)*16,y+Math.floor(q/2)*16,get(im,x,y));
  }
  await save('ctm/body.png',atlas);
  json('body.png.mcmeta',{ctm:{ctm_version:1,type:'CTM',layer:'SOLID',textures:['biggerreactorsrework:block/battery/cell/ctm/body'],extra:{connect_to:[{block:'biggerreactorsrework:battery_cell'}],connect_inside:true,ignore_states:true}}});
  json('connected/mapping.json',{version:1,texture_size:[16,16],block:'biggerreactorsrework:battery_cell',scope:'same cell block, all assembly/charge states',bits:{left:1,right:2,top:4,bottom:8,top_left:16,top_right:32,bottom_left:64,bottom_right:128},rule:'Diagonal bits count only when both adjacent cardinal neighbors are present. UV directions are relative to each rendered face.',variants:masks.map(mask=>({mask,file:`body_${mask.toString(16).padStart(2,'0')}.png`}))});
  await board('preview-connected.png',body,energy,variants);
  // Verify user artwork remains byte-for-byte untouched and CTM affects borders only.
  if(masks.length!==47)throw Error('Incorrect CTM variant count');
  if(!originalBody.equals(fs.readFileSync(path.join(TARGET,'body.png'))))throw Error('Original changed');
  if(!body.data.equals(variants.get(0).data))throw Error('Isolated CTM changes user artwork');
  for(const [m,im] of variants)for(let y=3;y<13;y++)for(let x=3;x<13;x++)if(get(body,x,y).join()!==get(im,x,y).join())throw Error('CTM changed core artwork');
  for(const f of frames)for(let y=0;y<16;y++)for(let x=0;x<16;x++)if(get(f,x,y)[3]&&!get(oldEnergy,x,y)[3])throw Error('Animation leaves old aperture');
  json('verification.json',{body_sha256:hash(originalBody),body_untouched:true,isolated_variant_identical:true,ctm_center_unchanged:true,bar_pixels:12,animation_frames:frames.length,frame_dimensions:[16,16],sheet_dimensions:[16,128],ctm_variants:masks.length,atlas_dimensions:[32,32],runtime_note:'CTM metadata requires the CTM renderer. Native integration uses connected/mapping.json; textures alone do not add game code.'});
  const manifest=[];
  function walk(p,rel=''){for(const e of fs.readdirSync(p,{withFileTypes:true})){const r=path.join(rel,e.name);if(e.isDirectory())walk(path.join(p,e.name),r);else manifest.push(r);}}
  walk(STAGE);fs.writeFileSync(path.join(ROOT,'manifest.json'),JSON.stringify(manifest,null,2));
  console.log(JSON.stringify({files:manifest.length,variants:47,bodyHash:hash(originalBody),stage:STAGE}));
}
async function apply(){
  const proof=JSON.parse(fs.readFileSync(path.join(STAGE,'verification.json'),'utf8'));
  if(hash(fs.readFileSync(path.join(TARGET,'body.png')))!==proof.body_sha256)throw Error('User body changed since preview; rebuild first');
  const files=JSON.parse(fs.readFileSync(path.join(ROOT,'manifest.json'),'utf8'));
  const backup=path.join(TARGET,'_backup_before_charge_bars_'+Date.now());fs.mkdirSync(backup,{recursive:true});
  for(const rel of files){
    const dest=path.resolve(TARGET,rel);if(!dest.startsWith(path.resolve(TARGET)+path.sep))throw Error('Path escapes target');
    if(fs.existsSync(dest)){fs.mkdirSync(path.dirname(path.join(backup,rel)),{recursive:true});fs.copyFileSync(dest,path.join(backup,rel));}
    fs.mkdirSync(path.dirname(dest),{recursive:true});fs.copyFileSync(path.join(STAGE,rel),dest);
    if(!fs.readFileSync(dest).equals(fs.readFileSync(path.join(STAGE,rel))))throw Error('Copy verification failed');
  }
  if(hash(fs.readFileSync(path.join(TARGET,'body.png')))!==proof.body_sha256)throw Error('Body was modified');
  console.log(JSON.stringify({updated:files.length,target:TARGET,backup,body_unchanged:true}));
}
(process.argv.includes('--apply')?apply():main()).catch(e=>{console.error(e);process.exit(1)});
