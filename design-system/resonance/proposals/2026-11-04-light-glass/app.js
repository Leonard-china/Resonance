'use strict';
const $=s=>document.querySelector(s);
const iconMarkup=new Map();
const icon=name=>{
  if(!iconMarkup.has(name)){
    const key=name.replace(/(^|-)([a-z0-9])/g,(_,dash,letter)=>letter.toUpperCase());
    const definition=lucide.icons[key];
    if(!definition)throw new Error('Unknown prototype icon: '+name);
    iconMarkup.set(name,lucide.createElement(definition,{'aria-hidden':'true','stroke-width':1.7}).outerHTML);
  }
  return iconMarkup.get(name);
};
const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const art=(id,cls='',alt='示例专辑封面')=>`<img src="assets/${id}.jpg" class="${cls}" alt="${alt}">`;
const btn=(text,action,ico='',cls='secondary')=>`<button class="${cls}" data-action="${action}">${ico?icon(ico):''}${text}</button>`;
const go=(text,id,ico='',cls='secondary')=>btn(text,'go:'+id,ico,cls);
const ib=(name,action,label)=>`<button class="icon-btn" data-action="${action}" aria-label="${label}">${icon(name)}</button>`;
const songs=[['晚风经过','林间电台','暮色来信','night','04:06'],['海边来信','北岸乐队','沿海公路','coast','03:48'],['日落之前','陈小满','远山','mountain','04:32'],['慢慢走','林间电台','暮色来信','forest','03:56'],['花开的时候','温柔宇宙','花间集','flower','04:18'],['另一片海','北岸乐队','沿海公路','coast','03:29'],['凌晨的山','山谷回声','远山','mountain','05:12'],['把今天留给自己','温柔宇宙','花间集','flower','03:42']];
const albums=[['夜行收藏','24 首 · 留给回家的路','night','playlist'],['沿海公路','18 首 · 风从海面吹来','coast','album'],['午后留白','32 首 · 慢一点也很好','forest','playlist'],['中文旧唱片','46 首 · 熟悉的声音','flower','playlist']];
const pages=[];const views={};
function register(id,title,group,source,render){pages.push({id,title,group,source});views[id]=render}
const head=(title,desc='',actions='')=>`<div class="page-head"><div><h1>${title}</h1>${desc?`<p>${desc}</p>`:''}</div>${actions?`<div class="buttons">${actions}</div>`:''}</div>`;
const section=(title,action='')=>`<div class="section-head"><h2>${title}</h2>${action}</div>`;
const field=(label,value='',type='text',placeholder='')=>`<label class="field">${label}<input type="${type}" value="${value}" placeholder="${placeholder}"></label>`;
const select=(label,opts)=>`<label class="field">${label}<select>${opts.map(o=>`<option>${o}</option>`).join('')}</select></label>`;
const toggle=(title,desc,on=true)=>`<div class="setting-row"><div class="row-copy"><h3>${title}</h3><p>${desc}</p></div><input class="switch" type="checkbox" aria-label="${title}" ${on?'checked':''}></div>`;
const row=(title,desc,target,ico='chevron-right')=>`<button class="setting-row link" data-action="go:${target}"><div class="row-copy"><h3>${title}</h3><p>${desc}</p></div>${icon(ico)}</button>`;
const info=(title,desc,ico='music-2')=>`<div class="setting-row">${icon(ico)}<div class="row-copy"><h3>${title}</h3><p>${desc}</p></div></div>`;
const note=t=>`<p class="note">${t}</p>`;
const panel=(title,body)=>`<section class="panel"><h2>${title}</h2>${body}</section>`;
const list=body=>`<div class="setting-list">${body}</div>`;
const tabs=(items,selected)=>`<div class="tabs" role="tablist">${items.map(([name,id])=>`<button role="tab" aria-selected="${id===selected}" class="${id===selected?'active':''}" data-action="go:${id}">${name}</button>`).join('')}</div>`;
const libraryTabs=id=>tabs([['歌曲','library'],['歌单','playlists'],['专辑','albums'],['歌手','artists']],id);
function tracks(ids=[0,1,2,3,4],header=true){return `<div class="track-list">${header?'<div class="list-header"><span>#</span><span>歌曲</span><span class="album-name">专辑</span><span>时长</span><span></span></div>':''}${ids.map((idx,i)=>{const s=songs[idx%songs.length];return `<div class="track ${currentSong===idx?'selected':''}"><span class="number">${currentSong===idx?icon('audio-lines'):String(i+1).padStart(2,'0')}</span><button class="track-main" data-action="song:${idx}">${art(s[3])}<span class="track-text"><strong>${s[0]}</strong><small>${s[1]}</small></span></button><span class="album-name">${s[2]}</span><span class="length">${s[4]}</span>${ib('ellipsis','track-menu:'+idx,'歌曲更多操作')}</div>`}).join('')}</div>`}
const cards=(items=albums,artist=false)=>`<div class="cards">${items.map(([name,sub,a,id])=>`<button class="album-card ${artist?'artist':''}" data-action="go:${id}">${art(a)}<h3>${name}</h3><p>${sub}</p></button>`).join('')}</div>`;
const hero=(title,sub,a,action='play',label='播放全部')=>`<section class="hero">${art(a,'hero-cover')}<div><h1>${title}</h1><p>${sub}</p><div class="buttons">${btn(label,action,action==='play'?'play':'arrow-right','primary pill')}${action==='play'?ib('heart','favorite','收藏')+ib('ellipsis','track-menu:0','更多操作'):''}</div></div></section>`;
const empty=(title,desc,action,ico='music-2')=>`<div class="empty"><div class="empty-icon">${icon(ico)}</div><h2>${title}</h2><p>${desc}</p><div class="buttons">${action}</div></div>`;
const formPage=(title,desc,body)=>head(title,desc)+`<div style="max-width:760px">${body}</div>`;
const modalPage=(title,body,actions)=>`<div class="modal-preview"><div class="modal-content"><h2>${title}</h2>${body}<div class="buttons">${btn('取消','back','','secondary')}${actions}</div></div></div>`;
let current='library',currentSong=0,playing=false,playerPane='lyrics',lyricIndex=2,historyStack=[],queue=[0,1,2,3,4],toastTimer,fromGallery=false;
const lyrics=['城市的灯一盏盏亮起','脚步慢下来','晚风经过你的窗前','也经过我的心间','让没说出口的想念','随着夜色走远'];
register('library','音乐库 · 歌曲','音乐库','LibraryShell.LibraryScreen',()=>head('音乐库','把喜欢的声音，留在身边。',go('导入','import','plus'))+libraryTabs('library')+`<div class="banner">${art('coast')}<div class="row-copy"><h3>今天，听一点新的</h3><p>每日推荐 · 为你准备的 5 首歌</p></div>${go('<span>开始探索</span>','recommendations','arrow-up-right')}</div>`+section('全部歌曲',go('搜索歌曲','search','search','quiet'))+tracks([0,1,2,3,4,5,6,7]));
register('playlists','音乐库 · 歌单','音乐库','LibraryShell.LibraryTab.Playlists',()=>head('音乐库','每一种心情，都有自己的歌单。',go('新建','create-playlist','plus'))+libraryTabs('playlists')+`<div class="banner">${icon('heart')}<div class="row-copy"><h3>我喜欢的音乐</h3><p>42 首 · 每一次心动都算数</p></div>${go('','favorites','chevron-right')}</div>`+section('我的歌单')+cards());
register('albums','音乐库 · 专辑','音乐库','LibraryShell.LibraryTab.Albums',()=>head('音乐库','从第一首到最后一首。')+libraryTabs('albums')+cards([['暮色来信','林间电台 · 2025','night','album'],['沿海公路','北岸乐队 · 2024','coast','album'],['远山','山谷回声 · 2025','mountain','album'],['花间集','温柔宇宙 · 2023','flower','album']])+section('最近加入')+tracks([1,3,4],false));
register('artists','音乐库 · 歌手','音乐库','LibraryShell.LibraryTab.Artists',()=>head('音乐库','那些陪伴你的声音。')+libraryTabs('artists')+cards([['林间电台','2 张专辑 · 18 首','night','artist'],['北岸乐队','3 张专辑 · 26 首','coast','artist'],['山谷回声','1 张专辑 · 12 首','mountain','artist'],['温柔宇宙','2 张专辑 · 20 首','flower','artist']],true));
register('playlist','歌单详情','音乐库','LibraryDetail.PlaylistDetail',()=>hero('夜行收藏','24 首歌曲，1 小时 36 分钟。\n把城市的喧闹留在耳机之外。','night')+section('歌单里的歌',go('管理歌曲','batch','list-checks','quiet'))+tracks([0,3,2,6,7]));
register('album','专辑详情','音乐库','LibraryDetail.AlbumDetail',()=>hero('沿海公路','北岸乐队 · 2024 · 10 首歌曲','coast')+section('全部曲目')+tracks([1,5,2,4,6]));
register('artist','歌手详情','音乐库','LibraryDetail.ArtistDetail',()=>hero('林间电台','曲库中有 18 首歌曲，来自 2 张专辑。','forest')+section('常听的歌')+tracks([0,3,7],false)+section('曲库中的专辑')+cards(albums.slice(0,2)));
register('favorites','我喜欢的音乐','音乐库','LibraryDetail.Favorites',()=>hero('我喜欢的音乐','42 首歌曲 · 从第一次心动，到单曲循环。','flower')+tracks([4,0,1,7,3,6]));
register('search','本地搜索','查找音乐','LibraryDestination.Discover / LibrarySearchIndex',()=>head('搜索音乐')+tabs([['本地曲库','search'],['在线歌曲','online'],['描述找歌','semantic']],'search')+`<label class="field">歌名、歌手或专辑<input class="search-input" id="library-search" value="晚风" placeholder="搜索你的音乐库"></label>`+section('搜索结果','<span class="muted" id="result-count">1 首</span>')+`<div id="search-results">${tracks([0])}</div>`);
register('online','在线搜索','查找音乐','OnlineSongSearch',()=>head('在线歌曲','先试听，喜欢再加入音乐库。')+tabs([['本地曲库','search'],['在线歌曲','online'],['描述找歌','semantic']],'online')+field('歌曲或歌手','','text','输入歌名或歌手')+`<div class="chips">${['全部歌源','网易云音乐','酷我音乐','酷狗音乐'].map((t,i)=>btn(t,'chip','',`chip ${i?'':'active'}`)).join('')}</div>`+tracks([0,1,2,3])+note('原型仅展示示例结果；试听、下载和歌源请求在原生版本中接入。'));
register('semantic','描述找歌','查找音乐','SemanticSongSearch',()=>head('描述你想听的歌','不记得歌名，也可以从感觉开始。')+tabs([['本地曲库','search'],['在线歌曲','online'],['描述找歌','semantic']],'semantic')+`<div class="form-grid"><label class="field">想听什么<textarea>适合傍晚沿着海边散步，节奏慢一点的歌</textarea></label>${btn('在曲库里找找','semantic-search','sparkles','primary')}</div>`+section('这样的声音，也许刚刚好')+tracks([1,5,0]));
register('recommendations','每日推荐','发现与推荐','RecommendationsScreen',()=>head('每日推荐','11 月 4 日 · 让今天有一点新鲜感',go('历史','recommendation-history','history'))+hero('听见另一种心情','从熟悉的喜好出发，遇见一些新声音。今天为你选了 5 首。','coast','play','播放这 5 首')+tracks([1,4,2,5,0])+note('推荐理由与点赞、跳过反馈在歌曲更多菜单中查看。'));
register('recommendation-history','推荐历史','发现与推荐','RecommendationHistoryScreen',()=>head('那些听过的推荐','喜欢的留下，错过的再听一次。')+tabs([['全部','recommendation-history'],['已喜欢','favorites']],'recommendation-history')+section('昨天，11 月 3 日','<span class="muted">5 首歌曲</span>')+tracks([3,4,6],false)+section('11 月 2 日')+tracks([1,2],false));
for(const [id,title] of [['player-cover','播放器 · 封面'],['player-lyrics','播放器 · 歌词'],['player-queue','播放器 · 队列']])register(id,title,'播放器','NowPlayingOverlay',()=>head('正在播放')+tracks([0]));
register('assistant','音乐助手 · 对话','音乐助手','AssistantScreen / AssistantMusicAttachment',()=>head('音乐助手','一起找歌，也一起整理音乐。',go('会话','assistant-history','history')+btn('新建','go:assistant-welcome','plus'))+`<div class="chat"><div class="bubble user">找几首适合晚上散步听的歌。</div><div class="response">${icon('sparkles')}<div><p>先听这几首吧。节奏舒缓，人声清楚，适合把脚步放慢一点。</p><div class="attachment">${tracks([0,1,2],false)}${btn('加入播放队列','append','list-plus','quiet')}</div><small>来自你的音乐库 · 示例回复</small></div></div><div class="bubble user">更偏安静一点。</div><div class="response">${icon('sparkles')}<p>好，那就从《晚风经过》开始。也可以告诉我，你更喜欢人声还是纯音乐。</p></div><div id="chat-new"></div></div><form class="chat-input" id="assistant-form">${go('','screenshot','paperclip','icon-btn')}<input aria-label="给音乐助手发消息" placeholder="聊聊音乐，或整理歌单">${btn('','send-chat','arrow-up','primary pill')}</form>`);
register('assistant-welcome','音乐助手 · 新会话','音乐助手','AssistantWelcome',()=>head('音乐助手','每一段对话，从一首歌开始。',go('历史','assistant-history','history'))+empty('今天想听些什么？','找一首歌、整理歌单，或者从一张截图开始。',go('聊聊音乐','assistant','sparkles','primary'),'sparkles')+`<div class="chips">${go('适合专心工作的歌','assistant','','chip')}${go('帮我整理重复歌曲','manager','','chip')}${go('从截图导入歌单','screenshot','','chip')}</div>`);
register('assistant-history','助手会话历史','音乐助手','AssistantScreen history dialog',()=>head('最近的对话','随时接着聊。',go('新会话','assistant-welcome','plus'))+list(row('适合晚上散步的歌','今天 20:18 · 4 条消息','assistant','message-circle')+row('把截图里的歌曲加入歌单','昨天 16:42 · 12 条消息','assistant','image')+row('周末的公路歌单','11 月 2 日 · 8 条消息','assistant','music-2')));
register('together','一起听 · 创建房间','一起听','TogetherScreen disconnected',()=>head('一起听','隔着距离，也在同一首歌里。')+tabs([['我来创建','together'],['加入朋友','join']],'together')+`<div class="split"><div>${hero('把这一刻分享出去','建一个房间，把邀请发给朋友。音乐与聊天，都在这里。','night','go:room','创建房间')}</div><div>${list(toggle('加入前由我确认','邀请仅分享给朋友。')+toggle('提前预备下一首','仅在非计费网络，减少等待。'))}${go('常听伙伴','partners','users','quiet')}${go('连接指南','room-help','circle-help','quiet')}</div></div>`);
register('join','一起听 · 加入朋友','一起听','TogetherScreen joining',()=>head('一起听','把朋友发来的邀请，粘贴在这里。')+tabs([['我来创建','together'],['加入朋友','join']],'join')+panel('加入朋友的房间',`<div class="form-grid"><label class="field">完整邀请<textarea id="invite-input" placeholder="支持完整邀请消息或 resonance-listen://…"></textarea></label>${btn('查看邀请','check-invite','arrow-right','primary')}<p class="note" id="invite-hint">加入前可先查看房间信息。</p></div>`));
register('room','一起听 · 房间','一起听','TogetherScreen connected / TogetherRoomComponents',()=>head('今晚慢慢听','示例房间 · 3 位朋友',btn('邀请','invite','link')+go('成员','members','users'))+`<section class="room-stage">${art('night')}<div><div class="presence">已同步 · 正在一起听</div><h2>晚风经过</h2><p class="muted">林间电台</p><div class="avatars"><span class="avatar">我</span><span class="avatar">林</span><span class="avatar">夏</span><small>都在这首歌里</small></div></div></section><div class="split"><section>${section('接下来一起听',go('完整队列','room-queue','list-music','quiet'))}${tracks([1,2,4],false)}${go('一起选歌','room-ai','sparkles','quiet')}</section><section>${section('说点什么')}<div class="room-chat" id="room-chat"><p><small>小林 · 20:14</small>这首前奏一出来，整个人都安静了。</p><p><small>小夏 · 20:15</small>下一首想听《海边来信》。</p></div><form class="chat-input" id="room-form"><input placeholder="发给房间里的朋友" aria-label="房间消息">${btn('','send-room','arrow-up','primary pill')}</form></section></div>`);
register('members','房间成员','一起听','TogetherRoomComponents / host controls',()=>head('房间里的朋友','3 人在线 · 控制音乐，也照顾彼此。')+list(info('我','房主 · 正在播放','crown')+info('小林','已连接 · 音频已就绪','headphones')+info('小夏','已连接 · 音频已就绪','headphones'))+section('加入申请')+`<div class="banner"><span class="avatar">陈</span><div class="row-copy"><h3>陈小满</h3><p>希望加入今晚的房间</p></div>${btn('允许','approve-member','check','primary')}${btn('拒绝','reject-member','','quiet')}</div>`);
register('room-queue','共享播放队列','一起听','TogetherRoomComponents expanded queue',()=>head('接下来一起听','房间成员可以一起添加和调整歌曲。',go('添加','search','plus'))+tracks([0,1,2,4,3,6])+note('示例队列。正式版的操作通过现有房间同步逻辑处理。'));
register('room-ai','房间 AI 选歌','一起听','TogetherAiQueue',()=>head('一起决定下一首','把大家的喜好，放进同一份歌单。')+panel('想听怎样的音乐？',`<div class="form-grid"><label class="field">选歌方向<textarea>适合朋友聊天，轻松温柔的人声</textarea></label>${btn('生成候选歌曲','room-candidates','sparkles','primary')}</div>`)+section('候选歌曲')+tracks([4,1,7],false)+btn('加入共享队列','append','list-plus','secondary'));
register('partners','常听伙伴','一起听','TogetherCompanionPanel',()=>head('常听伙伴','下一次听歌，不必从头认识。')+list(row('小林','上次一起听：昨天 · 夜行收藏','room','headphones')+row('小夏','上次一起听：11 月 2 日','room','headphones'))+section('陪伴设置')+list(toggle('接收伙伴邀请提醒','收到邀请后，由你决定是否加入。')));
register('room-help','共听连接指南','一起听','TogetherScreen connection help / details',()=>head('一起听连接指南')+list(info('创建并分享邀请','房主创建房间，使用顶栏的邀请入口分享完整消息。','link')+info('加入前检查邀请','确认房间和邀请来源，再建立连接。','shield-check')+info('等待歌曲准备好','收到音频后再对齐进度，避免播放不同的歌曲。','headphones')+info('遇到连接问题','检查网络，再返回房间重试；暂时退出不删除本地音乐。','wifi')));
const toolRows=[['导入音乐','扫描本机、转换文件、导入 B 站音频','import','folder-plus'],['歌单导入与导出','将音频、封面与歌单一起保存','archive','archive'],['曲库智能管家','发现重复版本、补全歌曲信息','manager','wand-sparkles'],['音乐品味画像','回顾你的聆听偏好','taste','chart-no-axes-combined'],['歌源','管理搜索和下载来源','sources','radio'],['下载与空间','查看下载、临时空间与重复文件','storage','hard-drive'],['音乐任务','导入、下载与补全进度','tasks','list-todo'],['设置','外观、播放与音乐助手','settings','settings-2'],['关于 Resonance','版本 0.6.5 · 本地优先','about','info']];
register('tools','工具','工具与管理','ProfileScreen',()=>head('工具','让音乐库井井有条。')+`<div class="split"><section>${section('音乐管理')}${list(toolRows.slice(0,3).map(x=>row(...x)).join(''))}${section('聆听与偏好')}${list(toolRows.slice(3,6).map(x=>row(...x)).join(''))}</section><section>${section('应用')}${list(toolRows.slice(6).map(x=>row(...x)).join(''))}${section('快速了解')}${list(row('使用教程','从导入第一首歌开始','tutorial','book-open'))}</section></div>`);
register('import','导入音乐','工具与管理','ImportScreen',()=>head('把音乐带进来','原始文件会保留，转换生成新的音频。')+`<div class="split">${panel('从本机导入',list(row('扫描音乐目录','选择有音乐的文件夹','import-progress','folder-search')+row('选择音频文件','MP3、FLAC 与其他支持的格式','import-progress','file-audio')+row('转换加密音乐','KGMA；KGG 依平台能力显示','conversion','audio-lines')))}${panel('从其他地方导入',list(row('歌单截图','在本机识别图片中的歌名','screenshot','scan-text')+row('B 站音频','粘贴视频链接并检查歌曲信息','video','link')+row('歌单文件','导入包含音乐和封面的歌单包','archive','archive')))}</div>`);
register('import-progress','导入进度','工具与管理','MusicTaskProgress / ImportProgress',()=>head('正在整理你的音乐','示例进度 · 扫描后逐首导入。')+panel('已处理 18 / 24 首',`<p class="muted">正在读取《海边来信》的封面与歌曲信息。</p><div class="progress-track"><span style="width:75%"></span></div><div class="buttons" style="margin-top:24px">${btn('暂停演示','cancel-import','pause')}${go('查看任务','tasks','list-todo')}</div>`)+section('已经完成')+tracks([0,3,4],false));
register('conversion','音频转换','工具与管理','ImportScreen KGMA/KGG flow',()=>formPage('转换音频','保留原文件，为你的音乐生成可播放副本。',panel('转换选项',`<div class="form-grid">${field('已选文件','晚风经过.kgma')}${select('输出质量',['MP3 · 320 kbps','MP3 · 192 kbps'])}${select('完成后加入',['音乐库','夜行收藏'])}${btn('开始转换演示','go:import-progress','audio-lines','primary')}</div>`)+note('原 MP3 不重新编码；提高码率不会恢复原音源细节。KGG 入口依桌面实际支持情况展示。')));
register('archive','歌单导入与导出','工具与管理','SyncScreen / SyncPackageDialog',()=>head('带着歌单走','歌曲、封面与歌单信息，一起保存。')+`<div class="split">${panel('导出歌单',`<div class="form-grid">${select('选择歌单',['夜行收藏 · 24 首','沿海公路 · 18 首','全部歌单'])}${toggle('包含音频文件','完整保存，便于迁移到另一台设备。')}${btn('预览导出内容','go:archive-preview','package','primary')}</div>`)}${panel('导入歌单',`<p class="muted">选择之前导出的歌单文件，检查内容后再导入。</p><div class="buttons" style="margin-top:24px">${btn('选择歌单文件','go:archive-preview','folder-open','secondary')}</div>`)}</div>`);
register('archive-preview','歌单包内容预览','工具与管理','SyncPackageDialog',()=>head('检查歌单内容','夜行收藏 · 示例歌单文件')+list(info('24 首歌曲','音频约 186 MB','music-2')+info('封面与歌词','封面 24 张 · 歌词 21 份','image')+info('已有歌曲','3 首可复用，不重复添加','check-check'))+note('导入前校验文件完整性和路径边界。此处为界面预览，不读写实际歌单包。')+btn('确认导入演示','go:import-progress','download','primary'));
register('tasks','音乐任务','工具与管理','MusicTasksScreen',()=>head('音乐任务','进度和结果，都有迹可循。')+`<div class="chips">${btn('全部','chip','','chip active')}${btn('进行中','chip','','chip')}${btn('已完成','chip','','chip')}</div>`+list(`<div class="setting-row"><div class="row-copy"><h3>导入：夜行收藏</h3><p>正在读取歌曲信息 · 18 / 24 首</p><div class="progress-track"><span style="width:75%"></span></div></div>${btn('暂停','cancel-import','pause')}</div>`+info('每日推荐已完成','今天 08:30 · 已找到 5 首歌曲','check-circle-2')+`<div class="setting-row"><div class="row-copy"><h3>《远山》封面获取失败</h3><p>网络连接中断，歌曲仍可播放。</p></div>${btn('重试','retry-task','rotate-cw')}</div>`));
register('sources','歌源管理','工具与管理','MusicSourcesScreen',()=>head('歌源','搜索、试听和下载的来源。',go('在线搜索','online','search'))+list(toggle('网易云音乐','内置公共歌源 · 示例开启')+toggle('酷我音乐','内置公共歌源 · 示例开启')+toggle('酷狗音乐','内置公共歌源 · 示例开启'))+section('添加其他歌源')+`<div class="form-grid">${field('歌源地址','','url','https://… / 订阅 JSON 地址')}<div class="buttons">${btn('从地址导入','mock-source','plus','primary')}${btn('选择 .js 文件','mock-source','file-code')}${go('来源偏好','source-settings','sliders-horizontal')}</div></div>`);
register('source-settings','来源偏好','工具与管理','MusicSourcesScreen source preferences',()=>formPage('来源偏好','让下载方式符合你的习惯。',list(toggle('保留原音轨格式','兼容时直接保存，关闭后转为 MP3。')+toggle('允许 B 站补充来源','公共歌源不可用时提供补充候选。'))+section('优先检索范围')+field('B 站 UID','','text','多个 UID 用逗号分隔')+note('自动候选仍排除 MV、现场及剪辑。账号名称不代表音质认证。')+btn('保存偏好','save','','primary')));
register('storage','下载与空间','工具与管理','MusicStorageScreen',()=>head('下载与空间','清楚知道音乐存在哪里。')+`<div class="split">${panel('已登记下载',`<p class="stat-total">2.46<small>GB</small></p><div class="progress-track"><span style="width:68%"></span></div>${list(info('已保存音乐','2.12 GB · 186 首','music-2')+info('临时下载','348 MB · 32 首','download'))}`)}${panel('空间设置',`<div class="form-grid">${field('临时下载上限（MB）','512','number')}${btn('保存上限','save','','primary')}${go('检查可释放内容','cleanup','hard-drive')}</div>`)}</div>`+note('只管理应用登记的下载。释放下载保留歌曲信息、收藏和歌单关系。当前播放的音频不参与清理。'));
register('cleanup','空间清理预览','工具与管理','MusicStorageScreen pending cleanup',()=>head('确认释放哪些下载','先检查，再清理。')+tracks([2,4,6])+list(info('可释放 36.8 MB','3 首临时下载；保留歌曲信息与收藏关系。','hard-drive'))+note('重新下载取决于原歌源是否可用。示例操作不会删除任何真实文件。')+btn('释放这 3 首的下载（演示）','cleanup-confirm','trash-2','secondary danger'));
register('manager','曲库智能管家','工具与管理','SmartManagerScreen',()=>head('让曲库更整齐','先查看建议，再决定如何处理。')+`<div class="split">${panel('值得整理的内容',list(row('重复版本','3 组歌曲可能重复','duplicates','copy')+row('缺少歌曲信息','8 首歌曲可补全歌手、专辑或封面','enrich','wand-sparkles')+row('标签格式','5 首歌曲的标题包含多余描述','edit-track','tag')))}${panel('先做一次检查',`<p class="muted">扫描曲库中的重复版本和缺失信息，所有修改都可先预览。</p><div class="buttons" style="margin-top:24px">${btn('开始检查演示','scan-manager','scan-search','primary')}</div>${note('扫描不会自动删除音频。标签写回须另行确认。')}`)}</div>`);
register('duplicates','重复版本检查','工具与管理','SmartManagerScreen duplicate review',()=>head('选择要保留的版本','找到 3 组可能重复的歌曲。')+panel('晚风经过 · 林间电台',`<label class="setting-row"><input type="radio" name="version" checked><div class="row-copy"><h3>晚风经过.flac</h3><p>FLAC · 28.6 MB · 已收藏</p></div><span class="tag">建议保留</span></label><label class="setting-row"><input type="radio" name="version"><div class="row-copy"><h3>晚风经过.mp3</h3><p>MP3 · 9.4 MB</p></div></label>`)+note('仅依据示例信息展示版本选择；不会自动删除未选版本。')+go('查看处理确认','delete-confirm','arrow-right','primary'));
register('taste','音乐品味画像','工具与管理','TasteRadarScreen',()=>head('你的音乐侧写','示例统计 · 最近 7 天')+`<div class="split">${panel('最近，常常听见你',`<p class="stat-total">12<small>小时 36 分钟</small></p><div class="stats-bars" role="img" aria-label="周一到周日示例聆听时长柱状图">${[45,68,38,80,55,95,75].map((v,i)=>`<div style="--h:${v}%"><span>${['一','二','三','四','五','六','日'][i]}</span></div>`).join('')}</div>`)}${panel('偏爱的声音',`<div class="chips"><span class="chip active">舒缓人声</span><span class="chip">独立流行</span><span class="chip">器乐</span></div><p>你常在晚上听歌，偏爱温柔的人声与留白。最近听得最多的是林间电台。</p>${list(info('最常听的歌手','林间电台 · 32 次','mic-vocal')+info('最常回访的歌单','夜行收藏 · 7 次','list-music'))}`)}</div>`+section('六维品味雷达',go('维度细则','taste-dimensions','radar','quiet'))+radar()+section('最近的常听曲目')+tracks([0,1,4],false));
const settingsRows=[['外观与动态效果','浅色材质、背景透色和减少动态','appearance','palette'],['播放与音效','音量平衡、音效、睡眠定时','playback','sliders-horizontal'],['歌词与翻译','歌词显示、语言、悬浮歌词','lyrics-settings','captions'],['模型与 API','模型连接与测试','ai-settings','sparkles'],['每日推荐计划','更新频率、后台与下载条件','schedule','calendar-clock'],['同步歌词生成','语音识别服务与处理额度','speech','mic'],['B 站账号','扫码或网页登录，提供补充音源','account','circle-user-round'],['使用教程','快速了解与分步操作','tutorial','book-open']];
register('settings','设置','设置与帮助','SettingsScreen / MusicFeatureSettingsSection',()=>head('设置','按你的方式，听音乐。')+`<div class="split"><section>${section('聆听体验')}${list(settingsRows.slice(0,3).map(x=>row(...x)).join(''))}${section('音乐库')}${list(row('本地歌词与音乐目录','选择目录与重新扫描','folders','folder-open'))}</section><section>${section('服务与助手')}${list(settingsRows.slice(3,7).map(x=>row(...x)).join(''))}${list(row(...settingsRows[7]))}</section></div>`);
register('appearance','外观与动态效果','设置与帮助','AppearanceSettings',()=>formPage('外观与动态效果','轻透的材质，清楚的内容。',panel('浅色外观',`<div class="banner">${art('night')}<div class="row-copy"><h3>跟随封面的氛围</h3><p>正文始终保持清晰对比。</p></div></div>${toggle('使用封面氛围色','从封面提取少量颜色，柔和铺在背景。')}${toggle('减少动态效果','关闭位移和缩放，保留即时状态反馈。',false)}${row('天气配色','位置、天气与兜底城市','weather','cloud-sun')}${note('本轮方案只展示浅色；不会改动原生应用中的主题设置。')}`)));
register('playback','播放与音效','设置与帮助','MusicFeatureSettingsSection playback',()=>formPage('播放与音效','让每首歌都舒适地衔接。',list(toggle('统一播放音量','分析响度并调整播放增益，不修改原音频。')+row('音效预设','当前：原声','effects','sliders-horizontal')+row('睡眠定时','未开启','sleep','moon')+row('音量平衡说明','了解分析、音量与原始文件','audio-help','circle-help'))+section('音量')+`<label class="field">播放音量 <span id="volume-label">70%</span><input type="range" value="70" aria-label="播放音量" data-range="volume"></label>`));
register('lyrics-settings','歌词与翻译','设置与帮助','TranslationControls / floating lyrics preferences',()=>formPage('歌词与翻译','把每一句，都看得清楚。',panel('显示方式',tabs([['原文','lyrics-settings'],['双语','lyrics-settings'],['译文','lyrics-settings']],'lyrics-settings')+`<div class="form-grid">${select('翻译目标语言',['简体中文','English','日本語'])}</div>`)+section('悬浮歌词')+list(toggle('启用悬浮歌词','离开播放器时，也能跟着歌词听。',false)+toggle('锁定歌词位置','点击穿透依平台支持。',false))+section('歌词文件')+list(row('本地歌词目录','自动查找音频旁的 KRC / LRC','folders','folder-open')+row('生成同步歌词','根据音频识别人声并生成时间轴','speech','mic'))));
register('ai-settings','模型与 API','设置与帮助','AiConnectionEditor',()=>formPage('模型与 API','按服务商规则计费，仅由明确操作发起请求。',panel('模型连接',`<div class="form-grid">${field('连接名称','我的音乐助手')}${field('API 地址','','url','https://服务商地址/v1')}${field('模型名称','','text','输入模型 ID')}${field('API Key','','password','此原型请勿输入真实密钥')}<div class="buttons">${btn('模拟连接测试','test-connection','plug','secondary')}${btn('保存演示配置','save','','primary')}</div><p id="connection-result" class="note">请使用占位内容；原型不存储或发送连接信息。</p></div>`)));
register('schedule','每日推荐计划','设置与帮助','MusicFeatureSettingsSection recommendations',()=>formPage('每日推荐计划','用合适的节奏，遇见新音乐。',list(toggle('自动推荐并下载','按照计划更新推荐。')+toggle('允许后台生成','桌面需保持运行，Android 由系统调度。')+toggle('仅非计费网络下载','无法确认网络状态时等待。'))+section('推荐方式')+`<div class="form-grid">${select('选歌方式',['歌源推荐','AI 精选'])}${field('每次推荐首数','5','number')}${field('间隔小时','24','number')}${field('同首歌曲不重复天数','7','number')}${btn('保存计划','save','','primary')}</div>`));
register('speech','同步歌词生成','设置与帮助','MusicFeatureSettingsSection speech',()=>formPage('同步歌词生成','识别人声，为歌曲生成对应的时间轴。',panel('语音识别配置',`<div class="form-grid">${field('百炼语音 API Key','','password','留空复用千问 Key；请勿填真实密钥')}${field('语音 WebSocket 地址','','url','填写服务商提供的地址')}${field('每月处理上限（分钟）','60','number')}${btn('保存演示配置','save','','primary')}</div>`)+note('原生版本仅在点击生成后上传音频，按音频时长计费，结果保存在本机。此原型不会上传音频。')));
register('account','B 站账号','设置与帮助','BilibiliLoginDialogs',()=>formPage('B 站账号','可选登录，为音频导入提供补充来源。',panel('连接你的账号',`<p class="muted">选择扫码登录，或在支持的平台打开网页登录。</p><div class="buttons" style="margin:24px 0">${btn('扫码登录演示','go:login-qr','qr-code','primary')}${btn('网页登录说明','web-login','external-link')}</div>${field('手动填写 Cookie','','password','原型请勿输入真实 Cookie')}${note('账号配置属于原生应用；这里仅展示界面。')}${btn('保存演示配置','save','','secondary')}`)));
register('login-qr','扫码登录状态','设置与帮助','BilibiliQrLoginDialog',()=>modalPage('使用 B 站扫码登录',empty('等待获取二维码','演示状态：未连接 B 站。正式版获取二维码后，可在此扫码并等待确认。',btn('模拟扫码成功','login-success','check','primary'),'qr-code'),btn('重新获取演示','qr-retry','rotate-cw')));
register('folders','本地音乐与歌词目录','设置与帮助','SettingsScreen local folders',()=>formPage('本地目录','歌曲与歌词，保存在你的设备上。',list(info('音乐库','由你选择的本机音乐目录','folder')+info('歌词查找','自动查找音频旁的 KRC / LRC 文件','file-text'))+section('自定义歌词目录')+`<div class="form-grid">${field('歌词目录','','text','未设置，使用自动查找')}<div class="buttons">${btn('选择目录（演示）','folder-select','folder-open')}${btn('恢复自动查找','folder-reset','rotate-ccw')}</div></div>`));
register('audio-help','音量平衡说明','设置与帮助','AudioBalanceHelpScreen',()=>head('音量平衡，让声音更从容')+list(info('先分析，再播放','根据整曲响度估算增益，让相邻歌曲的听感更接近。','audio-lines')+info('保留原始文件','只调整播放时的增益，不重新编码或写回原音频。','file-check')+info('音量仍由你掌握','系统音量和播放器音量继续生效，避免突然过响。','volume-2'))+section('播放设置')+go('返回播放偏好','playback','arrow-left','primary'));
register('tutorial','使用教程','设置与帮助','TutorialScreen / TutorialCatalog',()=>head('从第一首歌开始','随时回来，继续了解 Resonance。')+`<div class="banner">${icon('book-open')}<div class="row-copy"><h3>快速了解</h3><p>导入、聆听、助手、一起听</p></div>${go('开始','tutorial-chapter','arrow-right','primary')}</div>`+list([['导入你的音乐','扫描目录、保留原文件'],['播放与歌词','播放队列、双语歌词与悬浮'],['歌单与收藏','整理属于你的声音'],['音乐助手','描述找歌、截图导入'],['一起听','邀请朋友与共享队列'],['推荐与空间','留下喜欢的，管理临时下载'],['配置与费用','模型、语音和网络边界']].map(([a,b])=>row(a,b,'tutorial-chapter','chevron-right')).join('')));
register('tutorial-chapter','教程章节','设置与帮助','TutorialScreens quick / chapter',()=>head('导入你的第一首歌','第 1 步，共 3 步')+hero('选好音乐，剩下交给我们','从本机文件或音乐目录开始。先确认内容，再导入曲库。','coast','go:import','去导入音乐')+list(info('选择文件或目录','Android 使用系统文件选择器；桌面选择本机路径。','folder-open')+info('等待导入完成','已有 MP3 不重新编码，其他支持格式按流程处理。','list-checks')+info('回到音乐库播放','新加入的歌曲将出现在曲库里。','play')));
register('about','关于 Resonance','设置与帮助','AboutScreen / SponsorDialog',()=>head('关于 Resonance')+`<div class="empty"><div class="brand" style="justify-content:center;margin-bottom:25px"><span class="brand-mark">${icon('audio-lines')}</span>Resonance</div><h2>让音乐，与你共鸣。</h2><p>本地优先的跨平台音乐播放器。<br>Android · Windows · Linux</p><span class="tag">当前项目版本 0.6.5 / 39</span></div>`+list(row('版本更新','查看当前版本的功能说明','release-notes','file-text')+row('支持作者','了解赞助方式','sponsor','heart')+row('使用教程','从第一次导入开始','tutorial','book-open')));
register('release-notes','版本更新说明','设置与帮助','AboutScreen release notes',()=>head('版本 0.6.5','当前仓库版本 · 本轮方案不升级版本号')+list(info('播放队列管理','调整接下来要听的歌曲，支持下一首播放。','list-music')+info('曲库排序与筛选','更快找到想听的歌曲。','list-filter')+info('双语歌词舞台','原文和译文一起呈现。','captions'))+note('这是现有版本信息的界面示意，不代表本次已发布新版本。'));
register('sponsor','支持作者','设置与帮助','SponsorDialog',()=>modalPage('支持 Resonance',`<p>如果它让你的日常多了一点好听的声音，可以支持项目继续开发。</p>${note('此方案不放入真实收款码或支付入口。正式版本沿用现有赞助方式。')}`,btn('了解支持方式（演示）','sponsor-info','heart','primary')));
register('create-playlist','新建歌单','弹窗与操作','CreatePlaylistDialog / RenamePlaylistDialog',()=>modalPage('给新的歌单起个名字',`<div class="form-grid">${field('歌单名称','','text','例如：回家路上')}${field('描述（可选）','','text','留一点文字给这份心情')}</div>`,btn('创建歌单','create-playlist','plus','primary')));
register('edit-track','编辑歌曲信息','弹窗与操作','EditTrackDialog',()=>modalPage('歌曲信息',`<div class="form-grid">${field('歌曲名称','晚风经过')}${field('歌手','林间电台')}${field('专辑','暮色来信')}</div>${note('保存到曲库；写回音频标签需额外确认。')}`,btn('保存演示修改','save','','primary')));
register('batch','多选与歌单管理','弹窗与操作','BatchAddToPlaylistDialog / selection toolbar',()=>head('已选 3 首歌曲','选择要执行的操作。',btn('取消选择','go:playlist','x'))+tracks([0,1,4])+`<div class="buttons">${go('加入歌单','add-to-playlist','list-plus','primary')}${go('补全信息','enrich','wand-sparkles')}${go('删除确认','delete-confirm','trash-2','secondary danger')}</div>`);
register('add-to-playlist','添加到歌单','弹窗与操作','BatchAddToPlaylistDialog',()=>modalPage('把 3 首歌加入歌单',list(albums.map(([a,b])=>`<label class="setting-row"><input type="radio" name="playlist-choice" ${a==='夜行收藏'?'checked':''}><div class="row-copy"><h3>${a}</h3><p>${b}</p></div></label>`).join('')),btn('加入所选歌单','add-to-playlist','check','primary')));
register('enrich','批量补全歌曲信息','弹窗与操作','BatchEnrichDialog',()=>modalPage('补全缺少的信息',`<p class="muted">已选 8 首歌曲。先查看结果，再决定是否写入。</p>${list(toggle('歌名、歌手与专辑','优先保留已有完整信息。')+toggle('专辑封面','查找与歌曲匹配的封面。')+toggle('歌词','补齐可用的同步歌词。'))}${note('原生版本会使用网络；使用 AI 时按配置发起请求。')}`,btn('查看补全演示','go:enrich-result','wand-sparkles','primary')));
register('enrich-result','补全结果报告','弹窗与操作','BatchEnrichDialog report',()=>head('补全结果','示例：8 首歌曲，6 首已找到匹配。')+list(info('6 首可补全','4 张封面、2 首歌词与 3 处歌曲信息','check-circle-2')+info('2 首未找到可靠匹配','保留原信息，可稍后重试或手动编辑。','circle-help'))+section('预览变化')+tracks([0,1,4],false)+btn('保存到曲库（演示）','save','check','primary'));
register('screenshot','歌单截图导入','弹窗与操作','ScreenshotImportDialog',()=>head('从一张截图开始','图片在本机识别，无需 API Key。')+empty('选一张歌单截图','识别后检查歌名，再决定搜索或导入哪些歌曲。',btn('查看识别示例','go:screenshot-result','image-plus','primary'),'scan-text')+note('如切换为视觉模型识别，原生版本会按服务商配置上传图片并计费。'));
register('screenshot-result','截图识别结果','弹窗与操作','ScreenshotImportDialog recognized songs',()=>head('检查识别出的歌名','示例识别结果 · 3 首歌曲')+list(`<label class="setting-row"><input type="checkbox" checked><div class="row-copy"><h3>晚风经过</h3><p>林间电台</p></div></label><label class="setting-row"><input type="checkbox" checked><div class="row-copy"><h3>海边来信</h3><p>北岸乐队</p></div></label><label class="setting-row"><input type="checkbox" checked><div class="row-copy"><h3>日落之前</h3><p>陈小满 · 请核对歌手</p></div></label>`)+note('确认歌名后，搜索可用来源。识别结果不代表已下载。')+go('搜索选中的歌曲','online','search','primary'));
register('video','B 站音频导入','弹窗与操作','VideoImportDialog',()=>modalPage('导入 B 站音频',`<div class="form-grid">${field('视频链接','','url','粘贴视频链接或 BV 号')}${field('歌曲名称（可选）','','text','核对歌曲名称')}${select('加入歌单',['仅加入音乐库','夜行收藏','沿海公路'])}</div>${note('只在确认后解析和导入音频。此原型不请求视频内容。')}`,btn('解析演示','video-preview','link','primary')));
register('sleep','睡眠定时','弹窗与操作','SleepTimerDialog',()=>modalPage('听着音乐，慢慢入睡',`<p class="muted">时间到了，暂停播放。</p><div class="chips">${[15,30,45,60].map((n,i)=>btn(n+' 分钟','timer:'+n,'',`chip ${i===1?'active':''}`)).join('')}</div>${list(toggle('播完当前歌曲再暂停','不在歌曲中途打断。',false))}`,btn('开启 30 分钟定时','start-timer','moon','primary')));
register('effects','音效预设','弹窗与操作','AudioEffectDialog',()=>modalPage('找到舒服的听感',`<div class="chips">${['默认原声','纯净人声','澎湃低音','现场流行','震撼摇滚','3D 环绕','伴奏消音'].map((x,i)=>btn(x,'effect:'+x,'',`chip ${i?'':'active'}`)).join('')}</div><p class="muted" id="effect-description">原声：保留音源原本的声音。</p><div class="stats-bars" aria-label="音效示意，不是实际频响"><div style="--h:35%"><span>低频</span></div><div style="--h:48%"><span>中低频</span></div><div style="--h:60%"><span>中频</span></div><div style="--h:48%"><span>中高频</span></div><div style="--h:35%"><span>高频</span></div></div>`,btn('应用预设（演示）','save','check','primary')));
register('share','音乐分享卡','弹窗与操作','ShareCardDialog',()=>head('分享这一首','把心情，放进一张卡片。')+`<div class="panel" style="max-width:380px;margin:auto;text-align:center">${art('night','hero-cover')}<h2 style="margin:20px 0 5px">晚风经过</h2><p class="muted">林间电台</p><p style="margin:22px 0">晚风经过你的窗前<br>也经过我的心间</p><small>Resonance · 示例歌曲</small></div><div class="buttons" style="justify-content:center;margin-top:24px">${btn('保存分享卡（演示）','share-save','image-down','primary')}</div>`);
register('track-source','歌曲来源详情','弹窗与操作','TrackSourceDetails',()=>modalPage('歌曲来源',list(info('晚风经过','林间电台 · 暮色来信','music-2')+info('本地文件','MP3 · 320 kbps · 9.4 MB','file-audio')+info('封面与歌词','本地封面 · 已有同步歌词','captions'))+note('原型使用示例文件信息，不显示或读取本机文件路径。'),go('编辑歌曲信息','edit-track','pencil','primary')));
register('delete-confirm','删除确认','弹窗与操作','DeletePlaylistDialog / BatchDeleteTracksDialog',()=>modalPage('确认删除这 3 首歌曲？',`<p>这会影响真实音频文件。请检查所选内容，并区分移出歌单与删除文件。</p>${list(info('晚风经过、海边来信、花开的时候','示例选择 · 共 3 首','file-audio'))}${note('此原型不执行文件删除。原生版保留系统授权和二次确认。')}`,btn('仅演示确认','delete-demo','trash-2','secondary danger')));
register('playback-error','播放失败反馈','状态与反馈','PlaybackErrorDialog',()=>modalPage('这首歌暂时无法播放',`<p>《晚风经过》的音频文件可能已移动。你可以重新定位文件，或先播放下一首。</p><div class="buttons">${btn('重新定位（演示）','locate-demo','folder-search')}${btn('重试','retry-play','rotate-cw')}</div>`,btn('播放下一首','next','skip-forward','primary')));
register('empty-library','空曲库','状态与反馈','LibraryScreen empty / PlayingPlaceholderScreen',()=>head('音乐库')+empty('第一首歌，会是什么？','导入本机音乐，或者从一份歌单开始。你喜欢的声音，会慢慢聚在这里。',go('导入音乐','import','plus','primary')+go('了解使用方法','tutorial','book-open'),'music-2'));
register('loading','加载与缓冲','状态与反馈','PlaybackState feedback / MusicTaskProgress',()=>head('正在准备音乐')+`<div class="banner">${art('night')}<div class="row-copy"><h3>晚风经过</h3><p>正在准备音频，播放按钮暂不可用。</p></div><button class="icon-btn" disabled aria-label="正在准备">${icon('loader-circle')}</button></div>`+list(info('等待音频就绪','稍后开始播放，当前曲目与队列保持一致。','headphones'))+btn('模拟准备完成','go:player-lyrics','check','primary'));

const dimensionLabels=['流行敏锐度','复古情怀','小众探索度','情绪共鸣度','节奏活力感','流派多样性'];
function radar(){return `<div class="radar"><svg viewBox="0 0 420 300" role="img" aria-label="示例品味雷达：流行敏锐度65、复古情怀72、小众探索度80、情绪共鸣度90、节奏活力感48、流派多样性76"><polygon points="210,55 301,108 301,212 210,265 119,212 119,108" fill="none" stroke="#72809255"/><polygon points="210,90 271,125 271,195 210,230 149,195 149,125" fill="none" stroke="#72809233"/><polygon points="210,122 243,141 243,179 210,198 177,179 177,141" fill="none" stroke="#72809233"/><polygon points="210,92 275,123 283,202 210,254 166,185 141,120" fill="#a536492b" stroke="#a53649" stroke-width="2"/><g fill="#5c6572" font-size="12" text-anchor="middle"><text x="210" y="32">流行敏锐度</text><text x="347" y="102">复古情怀</text><text x="347" y="227">小众探索度</text><text x="210" y="294">情绪共鸣度</text><text x="72" y="227">节奏活力感</text><text x="72" y="102">流派多样性</text></g></svg></div>`}
register('taste-dimensions','品味雷达与维度','工具与管理','TasteRadarScreen dimension details',()=>head('每一种偏好，都有回响','示例画像 · 六个维度')+`<div class="split">${panel('你的品味雷达',radar())}${panel('维度细则',list(dimensionLabels.map((d,i)=>info(d,[65,72,80,90,48,76][i]+' / 100 · 示例分析','audio-lines')).join('')))}</div>`);
register('weather','天气配色与城市','设置与帮助','AppearanceSettings weather',()=>formPage('让天气带来一点颜色','仅在前台更新；定位不可用时使用所选城市。',panel('配色方式',`<div class="chips">${['封面配色','固定配色','天气配色'].map((x,i)=>btn(x,'chip','',`chip ${i===2?'active':''}`)).join('')}</div><p class="muted">晴天浅灰绿，雨雪冰蓝；阴天或获取失败时保留当前配色。</p>`)+section('天气与位置')+list(info('杭州 · 晴（示例）','兜底城市 · 未使用真实定位','sun'))+`<div class="form-grid">${field('搜索城市','','text','输入中文或英文城市名')}<div class="buttons">${btn('搜索示例','weather-search','search','primary')}${btn('定位并更新（演示）','weather-search','map-pin')}</div></div>`));
register('feedback','推荐反馈','弹窗与操作','Recommendation feedback reason/comment dialog',()=>modalPage('这一首，合你的心意吗？',`<p>反馈会帮助后续推荐更了解你的喜好。</p><div class="chips">${['喜欢这首','不是我的风格','听过太多次','版本不合适'].map((t,i)=>btn(t,'chip','',`chip ${i?'':'active'}`)).join('')}</div><label class="field">补充原因（可选）<textarea placeholder="想听更多怎样的声音？"></textarea></label>`,btn('保存反馈（演示）','save','check','primary')));
register('floating-lyrics','悬浮歌词','播放器','FloatingLyricsHost / DesktopFloatingLyrics',()=>head('悬浮歌词','独立于播放器窗口的轻量歌词条。')+`<div class="floating-preview"><p class="muted">桌面工作区预览</p><div class="floating-bar"><p>晚风经过你的窗前</p><small>The evening breeze passes your window</small><div class="buttons">${ib('lock','save','锁定位置')}${ib('settings-2','go:lyrics-settings','歌词设置')}${ib('x','back','关闭悬浮歌词')}</div></div></div>`+note('Android 需要系统悬浮窗权限。Windows / Linux 的点击穿透与窗口能力沿用平台实现。'));

function navItems(){return [['音乐库','library','library'],['音乐助手','assistant','sparkles'],['一起听','together','headphones'],['工具','tools','grid-2x2']]}
function activeNav(){const g=pages.find(p=>p.id===current)?.group;return g==='音乐助手'?'assistant':g==='一起听'?'together':['工具与管理','设置与帮助'].includes(g)?'tools':'library'}
function drawSidebar(){const nav=activeNav();$('#sidebar').innerHTML=`<div class="brand"><span class="brand-mark">${icon('audio-lines')}</span>Resonance</div><nav class="nav-group" aria-label="主导航">${navItems().map(([t,id,i])=>go(t,id,i,nav===id?'active':'')).join('')}</nav><div><div class="side-heading">你的音乐</div><nav class="nav-group">${go('我喜欢的音乐','favorites','heart')}${go('每日推荐','recommendations','disc-3')}</nav></div><div><div class="side-heading">我的歌单</div><nav class="nav-group">${albums.slice(0,3).map(([t,,a,id])=>`<button class="playlist-link" data-action="go:${id}">${art(a)}${t}</button>`).join('')}${go('新建歌单','create-playlist','plus')}</nav></div><div class="side-footer"><span class="avatar">R</span><div><strong>只属于你的音乐</strong><br><small>本地曲库</small></div></div>`}
function drawDock(){const s=songs[currentSong];$('#dock').className='dock';$('#dock').innerHTML=`<button class="now-track" data-action="open-player" aria-label="展开正在播放">${art(s[3])}<span><strong>${s[0]}</strong><small>${s[1]}</small></span></button><div class="transport">${btn('','shuffle','shuffle','shuffle quiet')}${btn('','previous','skip-back','previous quiet')}${btn('','toggle',playing?'pause':'play','play-button')}${btn('','next','skip-forward','quiet')}${btn('','repeat','repeat','repeat quiet')}</div><div class="dock-end">${ib('heart','favorite','收藏当前歌曲')}${ib('list-music','queue','播放队列')}<label aria-label="音量">${icon('volume-2')}<input aria-label="音量" type="range" value="70" data-range="volume"></label></div><nav class="bottom-nav" aria-label="主导航">${navItems().map(([t,id,i])=>go(t.replace('音乐助手','助手').replace('音乐库','音乐库'),id,i,activeNav()===id?'active':'')).join('')}</nav><div class="mini-progress"><span></span></div>`;$('#dock [data-action="toggle"]').setAttribute('aria-label',playing?'暂停':'播放');$('#dock [data-action="next"]').setAttribute('aria-label','下一首');$('#dock [data-action="previous"]').setAttribute('aria-label','上一首');$('#dock [data-action="shuffle"]').setAttribute('aria-label','随机播放');$('#dock [data-action="repeat"]').setAttribute('aria-label','循环播放')}
function drawPlayer(){const s=songs[currentSong],p=$('#player');p.classList.toggle('player-cover-only',playerPane==='cover');p.innerHTML=`<header class="player-top">${ib('chevron-down','close-player','收起播放器')}<span class="muted">正在播放</span><div class="tabs">${[['封面','cover'],['歌词','lyrics'],['队列','queue']].map(([t,id])=>btn(t,'pane:'+id,'',playerPane===id?'active':'')).join('')}</div>${ib('ellipsis','track-menu:'+currentSong,'播放器更多操作')}</header><div class="player-layout"><section class="cover-side">${art(s[3],'player-cover')}<div class="song-caption"><div><h1>${s[0]}</h1><p>${s[1]} · ${s[2]}</p></div>${ib('heart','favorite','收藏歌曲')}</div></section><section class="pane-side">${playerPane==='queue'?`<h2>接下来播放</h2><p class="note">${queue.length} 首歌曲 · 点击歌曲切换</p><div class="player-queue">${tracks(queue,false)}</div>`:`<div class="lyrics" id="lyrics-lines"><div class="lyric-track" style="transform:translateY(-${Math.max(0,lyricIndex-2)*68}px)">${lyrics.map((l,i)=>`<button class="lyric-line ${i===lyricIndex?'active':''}" data-action="lyric:${i}" aria-current="${i===lyricIndex}"><span>${l}<small style="visibility:${i===lyricIndex?'visible':'hidden'}">${['The city lights come on','Slow your steps','The evening breeze passes your window','And passes through my heart','The words we never said','Drift away into the night'][i]}</small></span></button>`).join('')}</div></div>`}</section></div><div class="player-controls"><label><span class="progress-label"><span id="elapsed">01:24</span><span>${s[4]}</span></span><input type="range" min="0" max="246" value="84" data-range="progress" aria-label="歌曲进度"></label><div class="transport">${ib('shuffle','shuffle','随机播放')}${ib('skip-back','previous','上一首')}${btn('','toggle',playing?'pause':'play','play-button')}${ib('skip-forward','next','下一首')}${ib('repeat','repeat','循环播放')}</div><div class="player-utilities">${go('音效','effects','sliders-horizontal','quiet')}${go('定时','sleep','moon','quiet')}${go('双语歌词','lyrics-settings','captions','quiet')}</div></div>`;$('#player [data-action="toggle"]').setAttribute('aria-label',playing?'暂停':'播放');}
// Cached icon markup avoids rescanning or replacing live SVGs on navigation.
const pageMotionPreference = matchMedia('(prefers-reduced-motion: reduce)');
let pageTransitionVersion=0;
const parkedIds=new WeakMap();
const pageLayers=()=>[...document.querySelectorAll('.page-viewport > main')];

function activatePage(layer,active){
  // A departing page remains visible, but has no focus targets or duplicate IDs.
  layer.inert=!active;
  if(active){
    layer.removeAttribute('aria-hidden');
    for(const [node,id] of parkedIds.get(layer)||[])node.id=id;
    parkedIds.delete(layer);
    layer.id='content';
  }else{
    layer.setAttribute('aria-hidden','true');
    if(!parkedIds.has(layer)){
      parkedIds.set(layer,[...layer.querySelectorAll('[id]')].map(node=>[node,node.id]));
      for(const [node] of parkedIds.get(layer))node.removeAttribute('id');
    }
    layer.removeAttribute('id');
  }
}
function stopPageTransition(){
  ++pageTransitionVersion;
  for(const layer of pageLayers()){
    if(layer.id!=='content'){layer.remove();continue;}
    layer.classList.remove('page-moving');
    layer.style.removeProperty('transform');
    layer.style.removeProperty('opacity');
  }
}
function transitionPage(previous,next,forward){
  const old=$('#content'),viewport=old.parentElement;
  if(old.dataset.page===next)return;
  const animate=old.childElementCount>0&&!next.startsWith('player-')&&
    !pageMotionPreference.matches&&!document.body.matches('.no-motion,.keyboard,.capture');
  const groups=[['library','playlists','albums','artists'],['library','assistant','together','tools']];
  const peers=groups.find(items=>items.includes(previous)&&items.includes(next));
  const direction=peers?Math.sign(peers.indexOf(next)-peers.indexOf(previous)):(forward?1:-1);
  const distance=parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--page-travel'));
  let incoming=pageLayers().find(layer=>layer.dataset.page===next);
  const fresh=!incoming;
  if(fresh){
    incoming=document.createElement('main');
    incoming.tabIndex=-1;
    incoming.dataset.page=next;
    incoming.innerHTML=views[next]();
    
    incoming.addEventListener('transitionend',event=>{
      if(event.target===incoming&&event.propertyName==='opacity'&&incoming.id!=='content'&&
         Number(getComputedStyle(incoming).opacity)===0)incoming.remove();
    });
  }
  const layers=pageLayers();
  // Retire only invisible layers: dropping a still-visible third page causes a flash.
  const invisible=layers.filter(layer=>layer!==old&&layer!==incoming&&Number(getComputedStyle(layer).opacity)===0);
  invisible.forEach(layer=>layer.remove());
  layers.filter(layer=>layer.isConnected).forEach(layer=>activatePage(layer,false));
  activatePage(incoming,true);
  if(fresh){
    if(animate){incoming.style.opacity='0';incoming.style.transform=`translateX(${direction*distance}px)`;}
    viewport.append(incoming);
  }
  if(!animate){stopPageTransition();return;}
  if(fresh)getComputedStyle(incoming).opacity; // Establish only the new layer's start state.
  const activeLayers=pageLayers();
  activeLayers.forEach(layer=>layer.classList.add('page-moving'));
  // CSS transitions retarget from the current presentation value, including reversal.
  // Do not cancel/restart keyframes, rebuild the old page, or run JS on every frame.
  incoming.style.opacity='1';
  incoming.style.transform='translateX(0px)';
  for(const layer of activeLayers){
    if(layer===incoming)continue;
    layer.style.opacity='0';
    layer.style.transform=`translateX(${-direction*distance}px)`;
  }
  const version=++pageTransitionVersion;
  const animations=activeLayers.flatMap(layer=>layer.getAnimations());
  Promise.allSettled(animations.map(animation=>animation.finished)).then(()=>{
    if(version===pageTransitionVersion)stopPageTransition();
  });
}
pageMotionPreference.addEventListener('change',()=>{if(pageMotionPreference.matches)stopPageTransition();});
function updateNavigation(){
  const active=activeNav();
  document.querySelectorAll('#sidebar nav[aria-label="主导航"] button, #dock .bottom-nav button').forEach(button=>{
    const selected=button.dataset.action==='go:'+active;
    button.classList.toggle('active',selected);
    if(selected)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');
  });
}
function render(id,remember=true){
  if(!views[id])id='library';
  const previous=current;
  if(remember&&current!==id)historyStack.push(current);
  current=id;
  closePlayer();
  const meta=pages.find(p=>p.id===id);
  $('#page-select').value=id;
  if(!$('#topbar').childElementCount){
    $('#topbar').innerHTML=`<div class="breadcrumbs">${ib('arrow-left','back','返回')}<span></span></div><div class="top-actions">${go('<span>搜索你的音乐</span>','search','search','search-pill')}${go('','tasks','list-todo','icon-btn')}</div>`;
    $('#topbar [data-action="go:tasks"]').setAttribute('aria-label','查看音乐任务');
    $('#topbar [data-action="go:search"]').setAttribute('aria-label','搜索音乐');
    drawSidebar();drawDock();
  }
  $('#topbar .breadcrumbs > span').textContent=meta.group;
  updateNavigation();
  transitionPage(previous,id,remember);
  if(id.startsWith('player-')){playerPane=id.split('-')[1];openPlayer();}
  const params=new URLSearchParams(location.search);params.set('page',id);
  window.history.replaceState(null,'','?'+params.toString());
  document.title=meta.title+' · Resonance 浅色方案';
}
function openPlayer(){stopPageTransition();drawPlayer();$('#player').classList.add('open');$('#player').setAttribute('aria-hidden','false');$('#content').inert=true;$('#sidebar').inert=true;$('#dock').inert=true;$('#topbar').inert=true;setTimeout(()=>{if(!document.body.classList.contains('capture'))$('#player [data-action="close-player"]')?.focus({preventScroll:true})},0)}
function setLyric(index){lyricIndex=index;const lines=document.querySelectorAll('.lyric-line');lines.forEach((n,i)=>{n.classList.toggle('active',i===index);n.setAttribute('aria-current',i===index);n.querySelector('small').style.visibility=i===index?'visible':'hidden'});const track=document.querySelector('.lyric-track');if(track)track.style.transform='translateY(-'+Math.max(0,index-2)*68+'px)'}
setInterval(()=>{if(playing&&$('#player').classList.contains('open')&&playerPane==='lyrics'&&!document.body.classList.contains('capture'))setLyric((lyricIndex+1)%lyrics.length)},3200);
function closePlayer(){$('#player').classList.remove('open');$('#player').setAttribute('aria-hidden','true');$('#content').inert=false;$('#sidebar').inert=false;$('#dock').inert=false;$('#topbar').inert=false}
function toast(message){clearTimeout(toastTimer);$('#toast').textContent=message;$('#toast').classList.add('visible');toastTimer=setTimeout(()=>$('#toast').classList.remove('visible'),2600)}
function showModal(title,body){const d=$('#modal');d.innerHTML=`<div class="modal-content"><h2>${title}</h2>${body}<div class="buttons">${btn('关闭','close-modal','','secondary')}</div></div>`;if(!d.open)d.showModal();}
function back(){if($('#player').classList.contains('open')){closePlayer();if(current.startsWith('player-'))render('library',false);return}render(historyStack.pop()||'library',false)}
function handle(action,el){const [a,v]=action.split(':');if(a==='go'){if($('#modal').open)$('#modal').close();render(v);return}switch(a){
case 'back':back();break;case 'open-player':openPlayer();break;case 'close-player':back();break;
case 'pane':playerPane=v;drawPlayer();break;
case 'toggle':playing=!playing;drawDock();if($('#player').classList.contains('open'))drawPlayer();break;
case 'play':playing=true;toast('开始播放示例队列（无声音）');drawDock();break;
case 'song':currentSong=Number(v);playing=true;drawDock();if($('#player').classList.contains('open'))drawPlayer();else{document.querySelectorAll('.track').forEach(t=>t.classList.remove('selected'));el.closest('.track')?.classList.add('selected')}break;
case 'next':case 'previous':currentSong=(currentSong+(a==='next'?1:songs.length-1))%songs.length;drawDock();if($('#player').classList.contains('open'))drawPlayer();break;
case 'queue':playerPane='queue';openPlayer();break;
case 'lyric':setLyric(Number(v));break;
case 'favorite':el.classList.toggle('danger');el.setAttribute('aria-pressed',el.classList.contains('danger'));toast(el.classList.contains('danger')?'已加入喜欢（演示）':'已取消喜欢（演示）');break;
case 'shuffle':case 'repeat':el.classList.toggle('danger');el.setAttribute('aria-pressed',el.classList.contains('danger'));toast((a==='shuffle'?'随机播放':'循环播放')+(el.classList.contains('danger')?'已开启':'已关闭')+'（演示）');break;
case 'append':queue.push(...[5,6,7].filter(x=>!queue.includes(x)));toast('已加入队列，共 '+queue.length+' 首（演示）');break;
case 'track-menu':showModal(songs[Number(v)][0],list(row('歌曲信息','查看并编辑歌曲信息','edit-track','pencil')+row('加入歌单','把它放进喜欢的歌单','add-to-playlist','list-plus')+row('来源详情','查看音频和歌词来源','track-source','info')+row('分享音乐卡','分享此刻的心情','share','share-2')));break;
case 'close-modal':$('#modal').close();break;
case 'send-chat':{const input=$('#assistant-form input');if(!input.value.trim()){input.focus();break}$('#chat-new').innerHTML+=`<div class="bubble user">${esc(input.value)}</div><div class="response" style="margin-top:22px">${icon('sparkles')}<p>这是本地交互演示。正式版会在这里给出与音乐库关联的回复。</p></div>`;input.value='';$('#chat-new').scrollIntoView({block:'nearest'});break}
case 'send-room':{const input=$('#room-form input');if(!input.value.trim())break;$('#room-chat').insertAdjacentHTML('beforeend',`<p><small>我 · 刚刚（演示）</small>${esc(input.value)}</p>`);input.value='';$('#room-chat').scrollTop=$('#room-chat').scrollHeight;break}
case 'chip':el.parentElement.querySelectorAll('.chip').forEach(x=>x.classList.remove('active'));el.classList.add('active');toast('筛选条件已切换（示例列表）');break;
case 'timer':el.parentElement.querySelectorAll('.chip').forEach(x=>x.classList.remove('active'));el.classList.add('active');document.querySelector('[data-action="start-timer"]').textContent=`开启 ${v} 分钟定时`;break;
case 'effect':el.parentElement.querySelectorAll('.chip').forEach(x=>x.classList.remove('active'));el.classList.add('active');$('#effect-description').textContent=v+'：已选择示例预设，点击应用确认。';break;
case 'check-invite':if(!$('#invite-input').value.includes('resonance-listen://')){$('#invite-hint').textContent='没有识别到邀请链接，请粘贴完整邀请消息。';$('#invite-hint').classList.add('danger')}else showModal('加入示例房间？',`<p>这是本地交互演示，不会建立真实连接。</p><div class="buttons">${go('确认加入演示','room','headphones','primary')}</div>`);break;
case 'invite':showModal('邀请朋友一起听',`<p>复制完整邀请，发送给想一起听的人。</p><label class="field" style="margin-top:20px">演示邀请<input readonly value="Resonance 邀请预览 · 不是有效房间链接"></label>${note('未生成真实房间或链接。')}${btn('模拟复制邀请','copy-invite','copy','primary')}`);break;
case 'test-connection':$('#connection-result').textContent='演示连接成功 · 未发起网络请求，未保存凭据。';$('#connection-result').classList.add('success');break;
case 'create-playlist':{const input=$('#content input');if(!input.value.trim()){input.focus();toast('先给歌单起个名字');break}toast('已创建“'+input.value.trim()+'”（演示）');break}
case 'approve-member':case 'reject-member':el.closest('.banner').innerHTML='<p>已'+(a==='approve-member'?'允许加入':'拒绝申请')+'（演示）</p>';break;
case 'retry-task':el.closest('.setting-row').querySelector('p').textContent='正在重试（演示，不请求网络）';el.disabled=true;break;
case 'cancel-import':el.innerHTML=icon('play')+'继续演示';el.dataset.action='resume-import';toast('已暂停演示进度');break;
case 'resume-import':el.innerHTML=icon('pause')+'暂停演示';el.dataset.action='cancel-import';toast('已继续演示进度');break;
case 'semantic-search':toast('已按描述展示 3 首示例候选');break;
case 'room-candidates':toast('已生成示例候选，可加入共享队列');break;
case 'scan-manager':toast('检查完成：3 组重复版本、8 首待补全（示例）');break;
case 'weather-search':toast('已展示示例城市；未请求定位或天气');break;
case 'save':toast('已应用到当前演示，未写入应用设置');break;
case 'copy-invite':toast('复制动作演示完成；未写入剪贴板');break;
case 'start-timer':toast(el.textContent+'（演示）');break;
case 'add-to-playlist':toast('已加入所选歌单（演示）');break;
case 'delete-demo':toast('确认流程演示完成；未删除文件');break;
case 'cleanup-confirm':showModal('释放下载确认',`<p>释放下载后保留歌曲信息和收藏。此演示不会删除文件。</p>${btn('确认演示','delete-demo','check','primary')}`);break;
case 'login-success':toast('登录成功状态演示；未登录真实账号');break;
case 'qr-retry':toast('二维码请求演示；未访问 B 站');break;
case 'web-login':toast('正式版会打开支持的网页登录入口');break;
case 'folder-select':toast('正式版会打开系统目录选择器');break;
case 'folder-reset':$('#content input').value='';toast('已恢复自动查找（演示）');break;
case 'video-preview':showModal('视频解析预览',`<p>正式版会在这里检查标题、时长和来源后再确认导入。</p>${go('查看导入进度示例','import-progress','download','primary')}`);break;
case 'mock-source':toast('导入流程演示；未下载或执行歌源脚本');break;
case 'share-save':toast('分享保存为演示；各页面图片可在总览中打开');break;
case 'sponsor-info':toast('正式版使用现有赞助入口，此处不发起支付');break;
case 'locate-demo':toast('正式版打开系统文件选择器重新定位');break;
case 'retry-play':render('loading');break;
default:toast('该操作为界面预览');}}
document.addEventListener('click',e=>{const el=e.target.closest('[data-action]');if(!el)return;e.preventDefault();handle(el.dataset.action,el)});
document.addEventListener('submit',e=>e.preventDefault());
document.addEventListener('input',e=>{if(e.target.id==='library-search'){const q=e.target.value.trim().toLowerCase(),ids=songs.map((s,i)=>s.join(' ').toLowerCase().includes(q)?i:-1).filter(i=>i>=0);$('#search-results').innerHTML=ids.length?tracks(ids):empty('没有找到这首歌','换个词，或试试在线搜索。',go('在线搜索','online','search','primary'),'search');$('#result-count').textContent=ids.length+' 首';}if(e.target.dataset.range==='progress'){$('#elapsed').textContent=String(Math.floor(e.target.value/60)).padStart(2,'0')+':'+String(e.target.value%60).padStart(2,'0')}if(e.target.dataset.range==='volume'&&$('#volume-label'))$('#volume-label').textContent=e.target.value+'%'});
document.addEventListener('keydown',e=>{document.body.classList.add('keyboard');stopPageTransition();if(e.key==='Escape'&&!$('#modal').open)back();if(e.key===' '&&!['INPUT','TEXTAREA','SELECT','BUTTON'].includes(e.target.tagName)){e.preventDefault();handle('toggle',null)}});
document.addEventListener('pointerdown',()=>document.body.classList.remove('keyboard'));
$('#page-select').innerHTML=[...new Set(pages.map(p=>p.group))].map(g=>`<optgroup label="${g}">${pages.filter(p=>p.group===g).map(p=>`<option value="${p.id}">${p.title}</option>`).join('')}</optgroup>`).join('');
$('#page-select').onchange=e=>render(e.target.value);
$('#device-toggle').onclick=()=>{document.body.classList.toggle('mock-phone');$('#device-toggle').textContent=document.body.classList.contains('mock-phone')?'桌面预览':'手机预览'};
$('#motion-toggle').onclick=()=>{document.body.classList.toggle('no-motion');if(document.body.classList.contains('no-motion'))stopPageTransition();$('#motion-toggle').setAttribute('aria-pressed',document.body.classList.contains('no-motion'));$('#motion-toggle').textContent=document.body.classList.contains('no-motion')?'恢复动态':'减少动态'};
const params=new URLSearchParams(location.search);if(params.has('capture'))document.body.classList.add('capture','no-motion');if(params.get('device')==='phone')document.body.classList.add('mock-phone');render(params.get('page')||'playlists',false);
window.prototype={pages,render,openPlayer,closePlayer,get current(){return current}};
