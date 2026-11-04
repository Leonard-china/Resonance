#!/usr/bin/env bash
set -euo pipefail
preview_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
preview_unit=resonance-light-ui-preview
preview_python="$(command -v python3)"
if systemctl --user is-active --quiet "$preview_unit.service"; then
  echo '预览已运行：http://127.0.0.1:8765/index.html'
  exit 0
fi
systemd-run --user --collect --unit="$preview_unit" \
  --description='Resonance light UI local preview' \
  --property=Restart=on-failure --property=RestartSec=2 \
  "$preview_python" -m http.server 8765 --bind 127.0.0.1 --directory "$preview_dir"
echo '原型：http://127.0.0.1:8765/index.html'
echo '全部图片：http://127.0.0.1:8765/gallery.html'
echo '停止：systemctl --user stop resonance-light-ui-preview.service'
