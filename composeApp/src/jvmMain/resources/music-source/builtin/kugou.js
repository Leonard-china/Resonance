const axios = require('axios');

module.exports = {
  platform: '酷狗音乐',
  version: '1.0.0',
  search: async function(query, page, type) {
    var p = page || 1;
    var limit = 20;
    var url = 'https://songsearch.kugou.com/song_search_v2?keyword=' + encodeURIComponent(query) + '&page=' + p + '&pagesize=' + limit + '&clientver=&platform=WebFilter';
    var res = await axios.get(url);
    var list = (res.data && res.data.data && res.data.data.lists) || [];
    return list.map(function(item) {
      var artwork = (item.Image || '').replace('{size}', '400');
      if (artwork && artwork.startsWith('http://')) {
        artwork = 'https://' + artwork.substring(7);
      }
      var isTrial = Boolean(item.IsFreePart === 1 || item.is_free_part === 1 || item.FailProcess === 4 || item.privilege === 10);
      var duration = item.Duration || (item.Grp && item.Grp[0] && item.Grp[0].Duration) || 0;
      return {
        id: item.FileHash || (item.Grp && item.Grp[0] && item.Grp[0].FileHash) || '',
        title: item.SongName || item.OriSongName || item.FileName || '',
        artist: item.SingerName || (item.Singers && item.Singers[0] && item.Singers[0].name) || '未知歌手',
        album: item.AlbumName || '',
        albumId: String(item.AlbumID || ''),
        duration: duration,
        artwork: artwork,
        isTrial: isTrial,
        isPreview: isTrial
      };
    });
  },
  getMediaSource: async function(musicItem, quality) {
    if (musicItem.isTrial || musicItem.isPreview) {
      throw new Error('此酷狗歌曲为试听片段，未提供完整免费音频');
    }
    var hash = musicItem.id;
    var url = 'https://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=' + hash;
    var res = await axios.get(url);
    if (res.data && (res.data.is_free_part === 1 || res.data.free_type === 1)) {
      throw new Error('此酷狗歌曲为试听片段，未提供完整免费音频');
    }
    var audioUrl = (res.data && res.data.url) || (res.data && res.data.backup_url && res.data.backup_url[0]) || '';
    if (!audioUrl) {
      throw new Error('此酷狗音频暂无免费直链，请尝试其他歌源');
    }
    return {
      url: audioUrl
    };
  },
  getLyric: async function(musicItem) {
    var hash = musicItem.id;
    var url = 'https://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=' + hash;
    var res = await axios.get(url);
    var lyric = (res.data && res.data.lyrics) || '';
    return {
      rawLrc: lyric
    };
  },
  getMusicInfo: async function(musicItem) {
    return musicItem;
  }
};
