#!/usr/bin/env bash
# One-time authorization on the deployment host, from a trusted checkout.
set -euo pipefail
(( EUID == 0 )) || { echo 'Run as root.' >&2; exit 1; }
owner=${1:?Specify the Jenkins executor user}
[[ $owner =~ ^[a-z_][a-z0-9_-]*$ && $owner != root ]]
id "$owner" >/dev/null
helper=/usr/local/sbin/baltic-bank-prepare-docker
install -o root -g root -m 0755 "$(dirname "$0")/provision.sh" "$helper"
rule=$(mktemp)
trap 'rm -f -- "$rule"' EXIT
printf '%s ALL=(root) NOPASSWD: %s ""\n' "$owner" "$helper" > "$rule"
chmod 0440 "$rule"
visudo -cf "$rule"
install -o root -g root -m 0440 "$rule" /etc/sudoers.d/baltic-bank-docker
