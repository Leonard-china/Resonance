const axios = require('axios');

module.exports = {
  platform: '酷我音乐',
  version: '1.0.0',
  search: async function(query, page, type) {
    var p = (page || 1) - 1;
    var limit = 20;
    var url = 'https://search.kuwo.cn/r.s?client=kt&all=' + encodeURIComponent(query) + '&pn=' + p + '&rn=' + limit + '&uid=794762528&ver=12.0.2&vipver=1&show_search_veri=1&isRetrieve=1&ft=music&encoding=utf8&rformat=json&mobi=1';
    var res = await axios.get(url);
    var list = (res.data && res.data.abslist) || [];
    return list.map(function(item) {
      var id = (item.DC_TARGETID || item.MUSICRID || '').replace('MUSIC_', '');
      var artwork = item.hts_MVPIC || '';
      if (!artwork && item.web_albumpic_short) {
        artwork = 'https://img4.kuwo.cn/star/albumcover/1080/' + item.web_albumpic_short;
      }
      return {
        id: String(id),
        title: item.SONGNAME || item.NAME || '',
        artist: item.ARTIST || item.FARTIST || '未知歌手',
        album: item.ALBUM || '',
        albumId: String(item.ALBUMID || ''),
        duration: parseInt(item.DURATION || '0', 10),
        artwork: artwork,
        isTrial: Boolean(item.payInfo && (item.payInfo.listen_fragment === '1' || item.payInfo.listen_fragment === 1))
      };
    });
  },
  getMediaSource: async function(musicItem, quality) {
    var id = String(musicItem.id || '').replace('MUSIC_', '');
    var url = 'https://antiserver.kuwo.cn/anti.s?type=convert_url&rid=' + id + '&format=mp3&response=url';
    var res = await axios.get(url, { responseType: 'text' });
    var audioUrl = typeof res.data === 'string' ? res.data.trim() : '';
    if (!audioUrl || !audioUrl.startsWith('http')) {
      throw new Error('未获取到音频地址');
    }
    return {
      url: audioUrl
    };
  },
  getLyric: async function(musicItem) {
    var id = musicItem.id;
    var url = 'https://m.kuwo.cn/newh5/singles/songinfoandlrc?musicId=' + id;
    var res = await axios.get(url);
    var list = (res.data && res.data.data && res.data.data.lrclist) || [];
    var lines = [];
    for (var i = 0; i < list.length; i++) {
      var item = list[i];
      var sec = parseFloat(item.time || '0');
      var m = Math.floor(sec / 60);
      var s = (sec % 60).toFixed(2);
      var timeStr = '[' + (m < 10 ? '0' : '') + m + ':' + (parseFloat(s) < 10 ? '0' : '') + s + ']';
      lines.push(timeStr + (item.lineLyric || ''));
    }
    return {
      rawLrc: lines.join('\n')
    };
  },
  getMusicInfo: async function(musicItem) {
    return musicItem;
  }
};
