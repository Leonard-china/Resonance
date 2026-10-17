'use strict';

// ==========================================
// 1. 页面交互逻辑封装 (支持防重与切页后重入)
// ==========================================
function initInteractive(root = document) {
  // 话题侧边栏展开/收起
  const toggle = root.querySelector ? root.querySelector('[data-topic-toggle]:not([data-bound])') : null;
  const sidebar = document.getElementById('topic-sidebar');
  if (toggle && sidebar) {
    toggle.dataset.bound = '1';
    toggle.addEventListener('click', () => {
      const open = toggle.getAttribute('aria-expanded') !== 'true';
      toggle.setAttribute('aria-expanded', String(open));
      sidebar.classList.toggle('open', open);
    });
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && sidebar.classList.contains('open')) {
        sidebar.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.focus();
      }
    });
  }

  // 手机 / 电脑展示模式切换
  const preview = root.querySelector ? root.querySelector('[data-preview-mode]:not([data-bound])') : null;
  if (preview) {
    preview.dataset.bound = '1';
    const previewButtons = [...preview.querySelectorAll('[data-preview]')];
    const narrow = matchMedia('(max-width: 720px)');
    let chosen = false;
    const select = (mode, focus = false) => {
      preview.dataset.previewMode = mode;
      previewButtons.forEach((button) => {
        const selected = button.dataset.preview === mode;
        button.setAttribute('aria-selected', String(selected));
        button.tabIndex = selected ? 0 : -1;
        if (selected && focus) button.focus();
      });
    };
    select(narrow.matches ? 'mobile' : 'desktop');
    narrow.addEventListener('change', () => {
      if (!chosen) select(narrow.matches ? 'mobile' : 'desktop');
    });
    previewButtons.forEach((button) => {
      button.addEventListener('click', () => {
        chosen = true;
        select(button.dataset.preview);
      });
      button.addEventListener('keydown', (event) => {
        if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
          event.preventDefault();
          chosen = true;
          const mode = event.key === 'Home' ? 'desktop' : event.key === 'End' ? 'mobile' : button.dataset.preview === 'mobile' ? 'desktop' : 'mobile';
          select(mode, true);
        }
      });
    });
  }

  // 校验值快速复制
  root.querySelectorAll('[data-copy]:not([data-bound])').forEach((button) => {
    button.dataset.bound = '1';
    button.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(button.dataset.copy);
        button.textContent = '已复制';
      } catch {
        button.textContent = '请长按上方校验值复制';
      }
    });
  });

  // 图片上传大小校验
  root.querySelectorAll('[data-image-input]:not([data-bound])').forEach((input) => {
    input.dataset.bound = '1';
    input.addEventListener('change', () => {
      const form = input.closest('form');
      const label = form ? form.querySelector('[data-image-name]') : null;
      const file = input.files[0];
      if (file && file.size > 3 * 1024 * 1024) {
        input.value = '';
        if (label) label.textContent = '图片不能超过 3 MB，请重新选择。';
      } else if (label) {
        label.textContent = file ? '已选择：' + file.name : '';
      }
    });
  });

  // 表单操作确认
  root.querySelectorAll('form[data-confirm]:not([data-bound])').forEach((form) => {
    form.dataset.bound = '1';
    form.addEventListener('submit', (event) => {
      if (!confirm(form.dataset.confirm)) event.preventDefault();
    });
  });

  // 表单防重复提交指示
  root.querySelectorAll('form[data-pending-label]:not([data-bound])').forEach((form) => {
    form.dataset.bound = '1';
    form.addEventListener('submit', () => {
      const button = form.querySelector('button[type="submit"], button:not([type])');
      if (button) {
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        button.textContent = form.dataset.pendingLabel;
      }
    });
  });
}

// 顶栏滚动吸顶阴影
const siteHeader = document.querySelector('.site-header');
const updateHeaderScroll = () => {
  if (siteHeader) {
    siteHeader.classList.toggle('scrolled', window.scrollY > 8);
  }
};
window.addEventListener('scroll', updateHeaderScroll, { passive: true });
updateHeaderScroll();
initInteractive(document);

// ==========================================
// 2. 停顿预加载与即时秒开加速 (Speculation Rules + Instant MPA)
// ==========================================
(function initInstantSpeed() {
  // 若用户处于弱网（2g）或主动开启节省流量，不自动提前拉取
  const conn = navigator.connection;
  const isSaveData = conn && (conn.saveData || /(2g)/.test(conn.effectiveType || ''));

  const cache = new Map(); // key: url.pathname + url.search -> { title, bodyClass, mainHtml, mainClass, mainId, flashHtml, time }
  const inFlight = new Map(); // key -> Promise
  const CACHE_TTL = 3 * 60 * 1000; // 3 分钟客户端新鲜度

  let progressBar = null;
  function showProgress() {
    if (!progressBar) {
      progressBar = document.createElement('div');
      progressBar.className = 'nav-progress';
      document.body.appendChild(progressBar);
    }
    progressBar.classList.remove('done');
    progressBar.classList.add('busy');
  }

  function hideProgress() {
    if (progressBar) {
      progressBar.classList.remove('busy');
      progressBar.classList.add('done');
      setTimeout(() => {
        if (progressBar && progressBar.classList.contains('done')) {
          progressBar.remove();
          progressBar = null;
        }
      }, 250);
    }
  }

  // 严格安全边界检查：仅允许同源公开只读展示页面
  function isSafePageUrl(url) {
    try {
      if (url.origin !== window.location.origin) return false;
      const path = url.pathname;
      // 排除下载二进制包、订阅处理、作者后台、管理路径
      if (path.startsWith('/download/') || path.startsWith('/subscription/')) return false;
      if (path.includes('/owner') || path.includes('/campaign') || path.includes('/manage')) return false;
      if (/\.(apk|exe|zip|tar|gz|png|jpg|webp|svg|css|js)$/i.test(path)) return false;
      return true;
    } catch {
      return false;
    }
  }

  // 1. 原生 Speculation Rules API 规则注入 (现代 Chrome / Edge / Android 原生支持)
  if (HTMLScriptElement.supports && HTMLScriptElement.supports('speculationrules')) {
    try {
      const spec = document.createElement('script');
      spec.type = 'speculationrules';
      spec.textContent = JSON.stringify({
        prefetch: [
          {
            source: 'list',
            urls: ['/', '/downloads', '/forum', '/support'],
            eagerness: 'moderate'
          }
        ]
      });
      document.head.appendChild(spec);
    } catch {
      // 优雅忽略
    }
  }

  // 2. 内存级快速抓取与解析
  async function fetchPage(url, priority = 'low') {
    const key = url.pathname + url.search;
    const cached = cache.get(key);
    if (cached && Date.now() - cached.time < CACHE_TTL) {
      return cached;
    }
    if (inFlight.has(key)) {
      return inFlight.get(key);
    }

    const promise = (async () => {
      try {
        const resp = await fetch(url.href, {
          priority,
          headers: { 'X-Requested-With': 'ResonanceInstant' }
        });
        if (!resp.ok) return null;
        const html = await resp.text();
        const doc = new DOMParser().parseFromString(html, 'text/html');
        const mainEl = doc.querySelector('main');
        const data = {
          title: doc.title,
          bodyClass: doc.body.className,
          mainHtml: mainEl ? mainEl.innerHTML : null,
          mainClass: mainEl ? mainEl.className : '',
          mainId: mainEl ? mainEl.id : 'main',
          flashHtml: doc.querySelector('.flash-stack') ? doc.querySelector('.flash-stack').innerHTML : '',
          time: Date.now()
        };
        cache.set(key, data);
        return data;
      } catch {
        return null;
      } finally {
        inFlight.delete(key);
      }
    })();

    inFlight.set(key, promise);
    return promise;
  }

  // 3. 页面无缝替换渲染
  function applyPage(data, url) {
    document.title = data.title;
    document.body.className = data.bodyClass;

    let mainEl = document.querySelector('main');
    if (mainEl && data.mainHtml !== null) {
      mainEl.className = data.mainClass;
      mainEl.id = data.mainId;
      mainEl.innerHTML = data.mainHtml;
    }

    let flashStack = document.querySelector('.flash-stack');
    if (data.flashHtml) {
      if (!flashStack && mainEl) {
        flashStack = document.createElement('div');
        flashStack.className = 'flash-stack';
        flashStack.setAttribute('role', 'status');
        mainEl.parentNode.insertBefore(flashStack, mainEl);
      }
      if (flashStack) flashStack.innerHTML = data.flashHtml;
    } else if (flashStack) {
      flashStack.remove();
    }

    // 更新顶栏主导航高亮项
    document.querySelectorAll('.main-nav a').forEach((a) => {
      try {
        const aUrl = new URL(a.href, window.location.origin);
        if (aUrl.pathname === url.pathname) {
          a.setAttribute('aria-current', 'page');
        } else {
          a.removeAttribute('aria-current');
        }
      } catch {}
    });

    // 重新激活当前页面所有交互
    initInteractive(mainEl || document);
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
    updateHeaderScroll();
  }

  // 4. 即时跳转导航
  async function navigate(url, push = true) {
    const key = url.pathname + url.search;
    let data = cache.get(key);

    if (!data || Date.now() - data.time >= CACHE_TTL) {
      showProgress();
      data = await fetchPage(url, 'high');
      hideProgress();
    }

    if (!data || !data.mainHtml) {
      // 容错降级：走原生传统跳转
      window.location.href = url.href;
      return;
    }

    if (push) {
      history.pushState({ key, href: url.href }, '', url.href);
    }

    if (document.startViewTransition) {
      document.startViewTransition(() => applyPage(data, url));
    } else {
      applyPage(data, url);
    }
  }

  // 5. 监听浏览器前进/后退 (瞬间呈现历史页面)
  window.addEventListener('popstate', (event) => {
    const currentUrl = new URL(window.location.href);
    const key = currentUrl.pathname + currentUrl.search;
    const cached = cache.get(key);
    if (cached) {
      if (document.startViewTransition) {
        document.startViewTransition(() => applyPage(cached, currentUrl));
      } else {
        applyPage(cached, currentUrl);
      }
    } else {
      navigate(currentUrl, false);
    }
  });

  // 6. 全局拦截同源超链接点击
  document.addEventListener('click', (event) => {
    // 忽略带修饰键的点击（如 Ctrl/Cmd 打开新标签页）或非左键点击
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    const anchor = event.target.closest('a[href]');
    if (!anchor || anchor.target === '_blank' || anchor.hasAttribute('download') || anchor.rel.includes('external')) {
      return;
    }
    const href = anchor.getAttribute('href');
    if (!href || href.startsWith('#')) return;

    try {
      const url = new URL(href, window.location.origin);
      if (!isSafePageUrl(url)) return;
      if (url.pathname === window.location.pathname && url.search === window.location.search) {
        // 同一页面直接平滑滚动到顶部或对应锚点
        if (!url.hash) {
          event.preventDefault();
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }
        return;
      }
      event.preventDefault();
      navigate(url, true);
    } catch {}
  });

  // 7. 用户停留停顿预加载（Idle Prefetching）
  // 当用户在当前页面停留超过 1 秒，说明首屏阅读停顿，静默预热站内核心页面
  function idlePrefetchKeyPages() {
    if (isSaveData) return;
    const pages = ['/', '/downloads', '/forum', '/support'];
    let idx = 0;
    function next() {
      if (idx >= pages.length) return;
      const target = pages[idx++];
      try {
        const u = new URL(target, window.location.origin);
        if (u.pathname !== window.location.pathname && isSafePageUrl(u)) {
          fetchPage(u, 'low');
        }
      } catch {}
      if ('requestIdleCallback' in window) {
        requestIdleCallback(next, { timeout: 2500 });
      } else {
        setTimeout(next, 300);
      }
    }
    next();
  }

  if (document.readyState === 'complete') {
    setTimeout(idlePrefetchKeyPages, 1000);
  } else {
    window.addEventListener('load', () => setTimeout(idlePrefetchKeyPages, 1000));
  }

  // 8. 交互即时触碰/悬停预加载 (Touchstart / Hover Prefetching)
  // 手指接触到屏幕或鼠标悬停在链接上通常到点击发生有 80~250ms，提前发起预取
  const triggerPrefetch = (event) => {
    const anchor = event.target.closest('a[href]');
    if (!anchor || anchor.target === '_blank' || anchor.hasAttribute('download')) return;
    try {
      const url = new URL(anchor.getAttribute('href'), window.location.origin);
      if (isSafePageUrl(url)) {
        fetchPage(url, 'low');
      }
    } catch {}
  };

  document.addEventListener('pointerenter', triggerPrefetch, { passive: true, capture: true });
  document.addEventListener('touchstart', triggerPrefetch, { passive: true, capture: true });
})();
