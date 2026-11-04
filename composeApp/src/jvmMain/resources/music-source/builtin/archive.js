// Netlabels' publicly downloadable recordings. Restricted/private items are not resolved.
const axios = require('axios');
function scalar(value) { return Array.isArray(value) ? value.join(' / ') : (value || ''); }
function seconds(value) {
  const parts = String(value || '0').split(':');
  return Math.round(parts.reduce(function(total, part) { return total * 60 + (parseFloat(part) || 0); }, 0));
}
function tracks(record, identifier) {
  const metadata = record.metadata || {};
  if (record.is_dark || record.is_restricted || metadata['access-restricted-item'] === 'true') return [];
  return (record.files || []).filter(function(file) {
    return !file.private && /\.(mp3|ogg|flac)$/i.test(file.name || '') && Number(file.size || 0) <= 512 * 1024 * 1024;
  }).filter(function(file, index, all) {
    // Prefer a single representation when the archive exposes MP3 and FLAC derivatives of one track.
    const stem = file.name.replace(/\.(mp3|ogg|flac)$/i, '');
    return !all.slice(0, index).some(function(other) { return other.name.replace(/\.(mp3|ogg|flac)$/i, '') === stem; });
  }).slice(0, 12).map(function(file) {
    return { id: identifier + ':' + file.name, title: scalar(file.title) || file.name.replace(/\.[^.]+$/, ''),
      artist: scalar(file.creator) || scalar(metadata.creator) || 'Netlabels', album: scalar(metadata.title),
      duration: seconds(file.length), artwork: 'https://archive.org/services/img/' + encodeURIComponent(identifier),
      url: 'https://archive.org/download/' + encodeURIComponent(identifier) + '/' + file.name.split('/').map(encodeURIComponent).join('/') };
  });
}
module.exports = {
  platform: 'Internet Archive · Netlabels', version: '1.0.0',
  search: async function(query, page) {
    const phrase = String(query).replace(/["\\]/g, ' ');
    const q = 'mediatype:audio AND collection:netlabels AND (title:"' + phrase + '" OR creator:"' + phrase + '")';
    const url = 'https://archive.org/advancedsearch.php?q=' + encodeURIComponent(q) + '&fl[]=identifier&rows=4&page=' + (page || 1) + '&output=json';
    const res = await axios.get(url);
    const docs = ((res.data || {}).response || {}).docs || [];
    const groups = await Promise.all(docs.map(async function(doc) {
      if (!/^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$/.test(doc.identifier || '')) return [];
      const metadata = await axios.get('https://archive.org/metadata/' + encodeURIComponent(doc.identifier));
      return tracks(metadata.data || {}, doc.identifier);
    }));
    return [].concat.apply([], groups);
  },
  getMusicInfo: async function(item) { return item; },
  getMediaSource: async function(item) {
    if (!/^https:\/\/archive\.org\/download\//.test(item.url || '')) throw new Error('档案音频地址不可用');
    return { url: item.url };
  }
};
