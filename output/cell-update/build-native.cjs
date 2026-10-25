const fs=require('fs'),path=require('path'),crypto=require('crypto');
const sharp=require('C:/Users/victo/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const {bodyVariant,energyVariant}=require('./build-universal.cjs');
const SRC='C:/Users/victo/Desktop/textures/textures/cell';
const OUT=path.join(__dirname,'native-cell');
const blank=(w=16,h=16)=>({w,h,data:Buffer.alloc(w*h*4)});
const get=(im,x,y)=>[...im.data.subarray((y*im.w+x)*4,(y*im.w+x)*4+4)];
const put=(im,x,y,c)=>im.data.set(c,(y*im.w+x)*4);
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function read(p){const {data,info}=await sharp(p).ensureAlpha().raw().toBuffer({resolveWithObject:true});return {data,w:info.width,h:info.height};}
async function save(name,im){const p=path.join(OUT,name);fs.mkdirSync(path.dirname(p),{recursive:true});await sharp(im.data,{raw:{width:im.w,height:im.h,channels:4}}).png().toFile(p);}
const suffix=m=>m===0?'none':m===15?'all':(m&4?'t':'')+(m&8?'b':'')+(m&1?'l':'')+(m&2?'r':'');
const norm=m=>(m&15)|((m&5)===5?m&16:0)|((m&6)===6?m&32:0)|((m&9)===9?m&64:0)|((m&10)===10?m&128:0);
(async()=>{
 fs.mkdirSync(OUT,{recursive:true});
 const originalBytes=fs.readFileSync(path.join(SRC,'body.png')),body=await read(path.join(SRC,'body.png'));
 fs.writeFileSync(path.join(OUT,'body.png'),originalBytes);
 const bodies=new Map();
 for(let m=0;m<16;m++){
  const im=bodyVariant(body,norm(m|240));bodies.set(m,im);
  await save('body/connected_'+suffix(m)+'.png',im);
 }
 const patches=[];
 for(let c=0;c<4;c++){
  const im=blank(),x0=c&1?13:0,y0=c&2?13:0;
  for(let y=y0;y<y0+3;y++)for(let x=x0;x<x0+3;x++)put(im,x,y,get(body,x,y));
  patches.push(im);await save('body/corner_'+['tl','tr','bl','br'][c]+'.png',im);
 }
 const staticBlue=await read(path.join(SRC,'energy.png'));
 for(const m of [0,4,8,12]){
  const stem=m===0?'energy':'energy/connected_'+suffix(m),still=blank(),anim=blank(16,128);
  const lo=m&4?0:6,hi=m&8?14:10;
  for(let y=lo;y<=hi;y+=2)for(let x=6;x<10;x++)put(still,x,y,get(staticBlue,x,8));
  await save(stem+'.png',still);
  for(let f=0;f<8;f++){
   const frame=energyVariant(m,f);
   for(let y=0;y<16;y++)for(let x=0;x<16;x++)put(anim,x,y+f*16,get(frame,x,y));
  }
  await save(stem+'_animated.png',anim);
  fs.writeFileSync(path.join(OUT,stem+'_animated.png.mcmeta'),JSON.stringify({animation:{width:16,height:16,frametime:4,interpolate:false,frames:[0,1,2,3,4,5,6,7]}},null,2)+'\n');
 }
 // Same four edge bits and four missing diagonal corners as ConnectedGlassCorners.
 const masks=[...new Set(Array.from({length:256},(_,m)=>norm(m)))];
 for(const m of masks){
  const actual={w:16,h:16,data:Buffer.from(bodies.get(m&15).data)};
  for(const [c,edges,diagonal]of [[0,5,16],[1,6,32],[2,9,64],[3,10,128]])if((m&edges)===edges&&!(m&diagonal)){
   for(let y=0;y<16;y++)for(let x=0;x<16;x++){const p=get(patches[c],x,y);if(p[3])put(actual,x,y,p);}
  }
  if(!actual.data.equals(bodyVariant(body,m).data))throw Error('Native corner reconstruction failed: '+m);
 }
 const files=[];
 function scan(p,rel=''){for(const e of fs.readdirSync(p,{withFileTypes:true})){const r=path.join(rel,e.name);if(e.isDirectory())scan(path.join(p,e.name),r);else files.push(r);}}
 scan(OUT);
 for(const f of files){
  if(f.endsWith('.png')){const m=await sharp(path.join(OUT,f)).metadata();if(m.width!==16||![16,128].includes(m.height))throw Error('Invalid native size '+f);}
  else if(f.endsWith('.mcmeta')){const m=JSON.parse(fs.readFileSync(path.join(OUT,f)));if(Object.keys(m).join()!=='animation')throw Error('External texture metadata');}
  else throw Error('Non-texture file in deliverable');
 }
 const proof={sourceBodySHA256:sha(originalBytes),files,faces:16,cornerPatches:4,staticEnergyVariants:4,animatedEnergyVariants:4,frames:8,validatedNativeCombinations:masks.length,nativeSource:['src/main/java/biggerreactorsrework/core/modular/block/IConnectedTexture.java','src/main/java/biggerreactorsrework/client/ConnectedGlassModels.java','src/main/java/biggerreactorsrework/client/ConnectedGlassCorners.java'],note:'Texture preparation only. BatteryCell currently does not implement IConnectedTexture. No game code changed.'};
 fs.writeFileSync(path.join(__dirname,'native-cell-verification.json'),JSON.stringify(proof,null,2));
 console.log(JSON.stringify({textureFiles:files.length,faces:16,corners:4,combinations:masks.length,originalBodyPreserved:true,externalMetadata:false}));
})().catch(e=>{console.error(e);process.exit(1)});
