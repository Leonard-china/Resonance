'use strict';

const toggle = document.querySelector('[data-topic-toggle]');
const sidebar = document.getElementById('topic-sidebar');
if (toggle && sidebar) {
  toggle.addEventListener('click', () => {
    const open = toggle.getAttribute('aria-expanded') !== 'true';
    toggle.setAttribute('aria-expanded', String(open));
    sidebar.classList.toggle('open', open);
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && sidebar.classList.contains('open')) {
      sidebar.classList.remove('open'); toggle.setAttribute('aria-expanded', 'false'); toggle.focus();
    }
  });
}

const preview = document.querySelector('[data-preview-mode]');
const previewButtons = [...document.querySelectorAll('[data-preview]')];
if (preview) {
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
  narrow.addEventListener('change', () => { if (!chosen) select(narrow.matches ? 'mobile' : 'desktop'); });
  previewButtons.forEach((button) => {
    button.addEventListener('click', () => { chosen = true; select(button.dataset.preview); });
    button.addEventListener('keydown', (event) => {
      if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
        event.preventDefault(); chosen = true;
        const mode = event.key === 'Home' ? 'desktop' : event.key === 'End' ? 'mobile' : button.dataset.preview === 'mobile' ? 'desktop' : 'mobile';
        select(mode, true);
      }
    });
  });
}

document.querySelectorAll('[data-copy]').forEach((button) => {
  button.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(button.dataset.copy);
      button.textContent = '已复制';
    } catch {
      button.textContent = '请长按上方校验值复制';
    }
  });
});

document.querySelectorAll('[data-image-input]').forEach((input) => {
  input.addEventListener('change', () => {
    const label = input.closest('form').querySelector('[data-image-name]');
    const file = input.files[0];
    if (file && file.size > 3 * 1024 * 1024) {
      input.value = ''; if (label) label.textContent = '图片不能超过 3 MB，请重新选择。';
    } else if (label) label.textContent = file ? '已选择：' + file.name : '';
  });
});

document.querySelectorAll('form[data-confirm]').forEach((form) => {
  form.addEventListener('submit', (event) => { if (!confirm(form.dataset.confirm)) event.preventDefault(); });
});

document.querySelectorAll('form[data-pending-label]').forEach((form) => {
  form.addEventListener('submit', () => {
    const button = form.querySelector('button[type="submit"], button:not([type])');
    if (button) { button.disabled = true; button.setAttribute('aria-busy', 'true'); button.textContent = form.dataset.pendingLabel; }
  });
});
