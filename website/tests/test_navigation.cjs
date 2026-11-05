const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const { test } = require('node:test');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../static/site.js'), 'utf8');
const code = source.slice(source.indexOf('// 4. 即时跳转导航'), source.indexOf('// 7. 用户停留停顿预加载'));
function fixture(transitions = false) {
  const pending = new Map(), applied = [], pushed = [], callbacks = [], listeners = {};
  const scope = { isSafePageUrl() { return true; }, cache: new Map(), CACHE_TTL: 180000, showProgress() {}, hideProgress() {},
    fetchPage(url) { return new Promise(resolve => pending.set(url.pathname, resolve)); },
    history: { pushState(_state, _title, url) { pushed.push(url); } },
    document: { addEventListener(name, cb) { listeners[name] = cb; }, ...(transitions ? { startViewTransition(cb) { callbacks.push(cb); return { skipTransition() {} }; } } : {}) },
    window: { location: new URL('https://fixture.invalid/'), scrollTo() {}, addEventListener(name, cb) { listeners[name] = cb; } },
    applyPage(_data, url) { applied.push(url.pathname); }, URL, Date };
  vm.createContext(scope); vm.runInContext(code + '\nglobalThis.navigate = navigate;', scope);
  return { ...scope, pending, applied, pushed, callbacks, listeners,
    url: name => new URL('https://fixture.invalid/' + name), data: () => ({ mainHtml: '<p>fixture</p>', time: Date.now() }) };
}
test('late first response never replaces the last click or history', async () => {
  const f = fixture(), first = f.navigate(f.url('forum')), last = f.navigate(f.url('downloads'));
  f.pending.get('/downloads')(f.data()); await last;
  f.pending.get('/forum')(f.data()); await first;
  assert.deepEqual(f.applied, ['/downloads']); assert.equal(f.pushed.length, 1);
});
test('a cached last click also invalidates pending requests and failed fallback', async () => {
  const f = fixture(); f.cache.set('/downloads', f.data());
  const first = f.navigate(f.url('forum')); await f.navigate(f.url('downloads'));
  f.pending.get('/forum')(null); await first;
  assert.deepEqual(f.applied, ['/downloads']); assert.equal(f.window.location.href, 'https://fixture.invalid/');
});
test('late ViewTransition callback cannot commit an older page or history', async () => {
  const f = fixture(true); f.cache.set('/forum', f.data()); f.cache.set('/downloads', f.data());
  await f.navigate(f.url('forum')); await f.navigate(f.url('downloads'));
  f.callbacks[1](); f.callbacks[0]();
  assert.deepEqual(f.applied, ['/downloads']); assert.equal(f.pushed.length, 1);
});
test('popstate cache hits invalidate an in-flight navigation without pushing history', async () => {
  const f = fixture(); f.cache.set('/downloads', f.data());
  const first = f.navigate(f.url('forum')); f.window.location.href = f.url('downloads').href;
  f.listeners.popstate(); f.pending.get('/forum')(f.data()); await first;
  assert.deepEqual(f.applied, ['/downloads']); assert.equal(f.pushed.length, 0);
});
test('last failed request keeps the native navigation fallback', async () => {
  const f = fixture(), current = f.navigate(f.url('forum'));
  f.pending.get('/forum')(null); await current;
  assert.equal(f.window.location.href, f.url('forum').href);
});

test('clicking the already displayed page cancels an older in-flight destination', async () => {
  const f = fixture(); f.window.location.href = f.url('downloads').href;
  const first = f.navigate(f.url('forum'));
  let prevented = false;
  const anchor = { target: '', rel: '', hasAttribute() { return false; }, getAttribute() { return '/downloads'; } };
  f.listeners.click({ button: 0, target: { closest() { return anchor; } }, preventDefault() { prevented = true; } });
  f.pending.get('/forum')(f.data()); await first;
  assert.equal(prevented, true); assert.deepEqual(f.applied, []); assert.equal(f.pushed.length, 0);
  assert.equal(f.window.location.href, f.url('downloads').href);
});
