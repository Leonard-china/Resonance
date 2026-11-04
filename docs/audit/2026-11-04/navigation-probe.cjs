const fs = require('fs');
const vm = require('vm');
const source = fs.readFileSync('website/static/site.js', 'utf8');
const begin = source.indexOf('async function navigate(url, push = true)');
const end = source.indexOf('// 5.', begin);
const pending = new Map();
const applied = [];
const pushed = [];
const scope = {
  cache: new Map(), CACHE_TTL: 180000,
  showProgress() {}, hideProgress() {},
  fetchPage(url) { return new Promise(resolve => pending.set(url.pathname, resolve)); },
  history: {pushState(data, _, url) { pushed.push(url); }},
  document: {}, window: {location: {}},
  applyPage(data, url) { applied.push(url.pathname); }, URL, Date,
};
vm.createContext(scope);
vm.runInContext(source.slice(begin, end) + '\nglobalThis.navigate = navigate;', scope);
(async () => {
  const first = scope.navigate(new URL('https://audit.invalid/forum'));
  const second = scope.navigate(new URL('https://audit.invalid/downloads'));
  pending.get('/downloads')({mainHtml: '<p>second</p>'}); await second;
  pending.get('/forum')({mainHtml: '<p>first</p>'}); await first;
  console.log(JSON.stringify({clickOrder: ['/forum', '/downloads'], applied, finalPage: applied.at(-1), pushed}, null, 2));
})();
