// Public QQ catalog and anonymous playback endpoints; paid/unavailable tracks remain unavailable.
const axios = require('axios');
const headers = { Referer: 'https://y.qq.com/', 'User-Agent': 'Mozilla/5.0 Resonance' };
module.exports = {
  platform: 'QQ 音乐', version: '1.0.0',
  search: async function(query, page) {
    const res = await axios.get('https://c.y.qq.com/soso/fcgi-bin/client_search_cp', {
      params: { w: query, format: 'json', p: page || 1, n: 20 }, headers: headers
    });
    const songs = (((res.data || {}).data || {}).song || {}).list || [];
    return songs.map(function(song) {
      const albumId = song.albummid || '';
      const paid = !!(song.pay && (song.pay.pay_play === 1 || song.pay.pay_play === '1'));
      return { id: song.songmid || song.mid || '', title: song.songname || song.name || '',
        artist: (song.singer || []).map(function(s) { return s.name; }).join(' / '),
        album: song.albumname || '', albumId: albumId, duration: song.interval || 0,
        artwork: albumId ? 'https://y.gtimg.cn/music/photo_new/T002R500x500M000' + albumId + '.jpg' : '',
        isTrial: paid, isPreview: paid };
    });
  },
  getMusicInfo: async function(item) { return item; },
  getMediaSource: async function(item) {
    if (item.isTrial || item.isPreview) throw new Error('此 QQ 音乐歌曲需要平台授权，未提供免费完整音频');
    const data = { req_0: { module: 'vkey.GetVkeyServer', method: 'CgiGetVkey',
      param: { guid: String(Math.floor(Math.random() * 10000000000)), songmid: [item.id], songtype: [0], uin: '0', loginflag: 1, platform: '20' } } };
    const res = await axios.get('https://u.y.qq.com/cgi-bin/musicu.fcg', { params: { data: JSON.stringify(data) }, headers: headers });
    const reply = ((res.data || {}).req_0 || {}).data || {};
    const entry = (reply.midurlinfo || [])[0] || {};
    if (!entry.purl || /^https?:/i.test(entry.purl)) throw new Error('QQ 音乐未提供可用的完整音频地址');
    const base = (reply.sip || []).find(function(url) { return /^https:\/\//.test(url); });
    if (!base) throw new Error('QQ 音乐音频服务器暂不可用');
    return { url: base + entry.purl };
  },
  getLyric: async function(item) {
    const res = await axios.get('https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg', {
      params: { songmid: item.id, format: 'json', nobase64: 1 }, headers: headers
    });
    return { rawLrc: (res.data || {}).lyric || '' };
  }
};
