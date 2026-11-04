#!/bin/sh
# Prepare services after extracting the source-only bundle into /opt/resonance.
# Deliberately leave production services stopped until existing data is restored.
set -eu
umask 077
test "$(id -u)" = 0 || { echo 'Run as root.' >&2; exit 1; }
ALIYUN_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
test "$ALIYUN_ROOT" = /opt/resonance || { echo 'Extract into /opt/resonance first.' >&2; exit 1; }
command -v python3 >/dev/null
command -v nginx >/dev/null
command -v systemctl >/dev/null
if ! id resonance >/dev/null 2>&1; then
    useradd --system --home-dir /opt/resonance --shell /usr/sbin/nologin resonance
fi
install -d -m 700 -o resonance -g resonance /var/lib/resonance-site /var/lib/resonance-relay
install -d -m 755 /var/lib/resonance-acme
# Code and virtualenvs must be readable by the unprivileged service account.
umask 022
python3 -m venv "$ALIYUN_ROOT/website/.venv"
"$ALIYUN_ROOT/website/.venv/bin/pip" install -r "$ALIYUN_ROOT/website/requirements.txt"
python3 -m venv "$ALIYUN_ROOT/services/relay/.venv"
"$ALIYUN_ROOT/services/relay/.venv/bin/pip" install -r "$ALIYUN_ROOT/services/relay/requirements.txt"
umask 077
if test ! -e "$ALIYUN_ROOT/website/.env"; then
    install -m 640 -o root -g resonance "$ALIYUN_ROOT/website/.env.example" "$ALIYUN_ROOT/website/.env"
    sed -i 's|^SITE_URL=.*|SITE_URL=https://leonard.asia|; s|^DATA_DIR=.*|DATA_DIR=/var/lib/resonance-site|' "$ALIYUN_ROOT/website/.env"
fi
install -m 644 "$ALIYUN_ROOT/website/deploy/resonance-site.service" /etc/systemd/system/
install -m 644 "$ALIYUN_ROOT/website/deploy/resonance-mail.service" /etc/systemd/system/
install -m 644 "$ALIYUN_ROOT/services/relay/deploy/resonance-relay.service" /etc/systemd/system/
for component in site mail; do
    install -d -m 755 "/etc/systemd/system/resonance-$component.service.d"
    install -m 644 "$ALIYUN_ROOT/services/deploy/aliyun/resonance-$component.override.conf" "/etc/systemd/system/resonance-$component.service.d/aliyun.conf"
done
systemctl daemon-reload
echo 'Prepared. Restore existing website data, .env and relay budget before starting services.'
echo 'Nginx/DNS/TLS and the existing production server were not changed.'
