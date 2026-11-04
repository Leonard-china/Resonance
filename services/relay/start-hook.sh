# Insert after PATH setup in /home/container/start.sh; keep all existing website commands.
if [ -x /home/container/resonance-relay/.venv/bin/python ] && [ -f /home/container/resonance-relay/supervise.py ]; then
  /home/container/resonance-relay/.venv/bin/python /home/container/resonance-relay/supervise.py --parent-pid "$$" &
else
  echo 'resonance_relay environment_missing; website continues' >&2
fi
