const axios = require('axios');

module.exports = {
  platform: '网易云音乐',
  version: '1.0.0',
  search: async function(query, page, type) {
    var p = page || 1;
    var limit = 20;
    var url = 'https://music.163.com/api/search/get/web?csrf_token=&s=' + encodeURIComponent(query) + '&type=1&offset=' + ((p - 1) * limit) + '&total=true&limit=' + limit;
    var res = await axios.get(url);
    var songs = (res.data && res.data.result && res.data.result.songs) || [];
    return songs.map(function(item) {
      var artists = (item.artists || []).map(function(a) { return a.name; }).join(' / ');
      var album = (item.album && item.album.name) || '';
      var artwork = (item.album && item.album.picUrl) || '';
      if (artwork && artwork.startsWith('http://')) {
        artwork = 'https://' + artwork.substring(7);
      }
      return {
        id: String(item.id),
        title: item.name || '',
        artist: artists || '未知歌手',
        album: album,
        albumId: item.album ? String(item.album.id) : '',
        duration: Math.round((item.duration || 0) / 1000),
        artwork: artwork ? artwork + '?param=500y500' : ''
      };
    });
  },
  getMediaSource: async function(musicItem, quality) {
    var id = musicItem.id;
    return {
      url: 'https://music.163.com/song/media/outer/url?id=' + id + '.mp3'
    };
  },
  getLyric: async function(musicItem) {
    var id = musicItem.id;
    var url = 'https://music.163.com/api/song/lyric?os=pc&id=' + id + '&lv=-1&kv=-1&tv=-1';
    var res = await axios.get(url);
    var lrc = (res.data && res.data.lrc && res.data.lrc.lyric) || '';
    return {
      rawLrc: lrc
    };
  },
  getMusicInfo: async function(musicItem) {
    return musicItem;
  }
};
