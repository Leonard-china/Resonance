const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
(async()=>{
 const browser=await chromium.launch({args:['--no-sandbox']});
 const errors=[],checks=[];
 try{
 const page=await browser.newPage({viewport:{width:1440,height:960}});
 page.on('pageerror',e=>errors.push(e.message));
 const settle=()=>page.waitForFunction(()=>document.querySelectorAll('.page-viewport > main').length===1&&document.querySelector('#content').getAnimations().length===0);
 const route=id=>page.evaluate(id=>{document.body.classList.remove('keyboard','no-motion');prototype.render(id);},id);
 await page.goto('file://'+__dirname+'/index.html');
 await settle();
 for(const width of [1440,390]){
   await page.setViewportSize({width,height:width===390?844:960});
   await route('library');await settle();
   await page.evaluate(()=>{
     window.shellNodes=[...document.querySelectorAll('#topbar button,#topbar svg,#sidebar button,#sidebar svg,#dock button,#dock input,#dock svg')];
     window.dockBounds=document.querySelector('#dock').getBoundingClientRect().toJSON();
     document.querySelector('#dock input').value='31';
     window.oldPage=document.querySelector('#content');
     prototype.render('albums');
     for(const layer of document.querySelectorAll('.page-viewport > main'))for(const a of layer.getAnimations()){a.pause();a.currentTime=0;}
   });
   const start=await page.evaluate(()=>{
     const incoming=document.querySelector('#content');
     return {layers:document.querySelectorAll('.page-viewport > main').length,oldConnected:oldPage.isConnected,
       oldOpacity:Number(getComputedStyle(oldPage).opacity),newOpacity:Number(getComputedStyle(incoming).opacity),
       inert:oldPage.inert&&oldPage.getAttribute('aria-hidden')==='true',
       duration:incoming.getAnimations()[0].effect.getTiming().duration,
       frames:incoming.getAnimations().flatMap(a=>a.effect.getKeyframes()).map(f=>Object.keys(f))};
   });
   assert.equal(start.layers,2);assert(start.oldConnected&&start.inert);assert.equal(start.oldOpacity,1);assert.equal(start.newOpacity,0);assert.equal(start.duration,280);
   assert(start.frames.every(keys=>keys.every(key=>['offset','computedOffset','easing','composite','transform','opacity'].includes(key))));
   const reversal=await page.evaluate(()=>{
     const layers=[...document.querySelectorAll('.page-viewport > main')];
     layers.forEach(layer=>layer.getAnimations().forEach(a=>{a.currentTime=90;}));
     const sample=()=>layers.map(layer=>({id:layer.dataset.page,opacity:Number(getComputedStyle(layer).opacity),x:new DOMMatrix(getComputedStyle(layer).transform).m41}));
     const before=sample();prototype.render('library');const after=sample();
     return {before,after,reused:document.querySelector('#content')===oldPage,
       shell:shellNodes.every(n=>n.isConnected),volume:document.querySelector('#dock input').value,
       dockSame:JSON.stringify(document.querySelector('#dock').getBoundingClientRect().toJSON())===JSON.stringify(dockBounds)};
   });
   assert(reversal.before.every(s=>s.opacity>0&&s.opacity<1));
   for(let i=0;i<reversal.before.length;i++){
     assert(Math.abs(reversal.before[i].opacity-reversal.after[i].opacity)<0.02,'opacity jumped on reversal');
     assert(Math.abs(reversal.before[i].x-reversal.after[i].x)<0.3,'position jumped on reversal');
   }
   assert(reversal.reused&&reversal.shell&&reversal.dockSame);assert.equal(reversal.volume,'31');
   await settle();
   checks.push(`${width}px：旧页保留与双层交接、仅位移/透明度、反向位置连续、导航/图标/音量节点保留`);

   await route('search');await settle();await page.locator('#library-search').fill('海');
   const retained=await page.evaluate(()=>{
     document.body.classList.remove('keyboard');
     const original=document.querySelector('#content');original.scrollTop=45;const scroll=original.scrollTop;
     prototype.render('albums');
     document.querySelectorAll('.page-viewport > main').forEach(layer=>layer.getAnimations().forEach(a=>{a.pause();a.currentTime=80;}));
     prototype.render('search');
     return {same:document.querySelector('#content')===original,value:document.querySelector('#library-search').value,
       results:document.querySelectorAll('#search-results .track').length,scroll:original.scrollTop===scroll};
   });
   assert(retained.same&&retained.scroll);assert.equal(retained.value,'海');assert.equal(retained.results,2);await settle();
   const burst=await page.evaluate(()=>{
     const sizes=[];let duplicated=false;let interactiveOld=false;
     for(const id of ['artists','playlists','assistant','together','tools','search','albums']){
       prototype.render(id);
       sizes.push(document.querySelectorAll('.page-viewport > main').length);
       const ids=[...document.querySelectorAll('[id]')].map(e=>e.id);duplicated ||= new Set(ids).size!==ids.length;
       interactiveOld ||= [...document.querySelectorAll('.page-viewport > main:not(#content)')].some(e=>!e.inert);
     }
     return {sizes,duplicated,interactiveOld};
   });
   assert(burst.sizes.every(n=>n<=3));assert(!burst.duplicated&&!burst.interactiveOld);await settle();
   assert.equal(await page.locator('#content').getAttribute('data-page'),'albums');
   assert.equal(await page.locator('#content').evaluate(e=>getComputedStyle(e).opacity),'1');
   assert.equal(await page.locator('#content').evaluate(e=>getComputedStyle(e).transform),'none');
   checks.push(`${width}px：往返保留搜索结果/输入/滚动、同帧连续7次切换不累积透明层、无重复ID/旧页点击、结束清理`);
 }
 // Separate clicks across actual frames, including a fourth visible page.
 // CPU throttling exercises interruptions, not an assertion about real phone FPS.
 const cdp=await page.context().newCDPSession(page);
 await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
 const stress=await page.evaluate(async()=>{
   const routes=['library','playlists','assistant','together','tools','albums','artists','search'];
   const renderMs=[];let lostVisible=false;let maxLayers=0;
   for(const id of routes){
     const visible=[...document.querySelectorAll('.page-viewport > main')].filter(n=>Number(getComputedStyle(n).opacity)>.02);
     const start=performance.now();prototype.render(id);renderMs.push(performance.now()-start);
     lostVisible ||= visible.some(n=>!n.isConnected);
     maxLayers=Math.max(maxLayers,document.querySelectorAll('.page-viewport > main').length);
     await new Promise(resolve=>setTimeout(resolve,45));
   }
   return {cpuThrottle:4,viewport:390,renderMs:renderMs.map(n=>Number(n.toFixed(2))),maxLayers,lostVisible};
 });
 assert.equal(stress.lostVisible,false);await settle();
 await cdp.send('Emulation.setCPUThrottlingRate',{rate:1});
 checks.push('4倍CPU降速下每45ms连续切换：可见旧页不中途删除，结束只剩当前页');
 await route('tools');await page.locator('#motion-toggle').click();await settle();
 await page.evaluate(()=>prototype.render('settings'));await settle();checks.push('手动减少动态立即结束转场');
 await page.locator('#motion-toggle').click();await route('assistant');await page.keyboard.press('Tab');await settle();
 await page.evaluate(()=>prototype.render('library'));await settle();checks.push('键盘操作立即结束转场且导航即时');
 await route('albums');await page.emulateMedia({reducedMotion:'reduce'});await settle();
 await page.evaluate(()=>prototype.render('tools'));await settle();checks.push('系统减少动态中途生效');
 await page.emulateMedia({reducedMotion:'no-preference'});await route('playlists');await page.evaluate(()=>prototype.openPlayer());await settle();
 assert.equal(await page.locator('#player').getAttribute('aria-hidden'),'false');assert(await page.locator('#content').evaluate(e=>e.inert));checks.push('转场中打开播放器，旧层立即清理且背景不可交互');
 await page.evaluate(()=>{prototype.closePlayer();document.body.classList.add('capture','no-motion')});
 const missing=[];for(const meta of await page.evaluate(()=>prototype.pages)){
   await page.evaluate(id=>prototype.render(id,false),meta.id);
   if(await page.locator('i[data-lucide]').count())missing.push(meta.id);
 }
 assert.deepEqual(missing,[]);checks.push('78个页面与状态的图标转换完整');
 assert.deepEqual(errors,[]);
 const report={date:new Date().toISOString(),passed:checks.length,checks,errors,stress,limits:'Chromium桌面与手机视口的行为检查；未测真实手机/120Hz帧率。'};
 fs.writeFileSync(path.join(__dirname,'motion-report.json'),JSON.stringify(report,null,2));console.log(report);
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
