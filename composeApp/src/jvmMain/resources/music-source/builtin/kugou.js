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
      return {
        id: item.FileHash || (item.Grp && item.Grp[0] && item.Grp[0].FileHash) || '',
        title: item.SongName || item.OriSongName || item.FileName || '',
        artist: item.SingerName || (item.Singers && item.Singers[0] && item.Singers[0].name) || '未知歌手',
        album: item.AlbumName || '',
        albumId: String(item.AlbumID || ''),
        duration: item.Duration || 0,
        artwork: artwork
      };
    });
  },
  getMediaSource: async function(musicItem, quality) {
    var hash = musicItem.id;
    var url = 'https://m.kugou.com/app/i/getSongInfo.php?cmd=playInfo&hash=' + hash;
    var res = await axios.get(url);
    var audioUrl = (res.data && res.data.url) || (res.data && res.data.backup_url && res.data.backup_url[0]) || '';
    if (!audioUrl) {
      throw new Error('此酷狗音频暂无免费直链，请尝试其他歌源');
    }
    if (audioUrl.startsWith('http://')) {
      audioUrl = 'https://' + audioUrl.substring(7);
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
