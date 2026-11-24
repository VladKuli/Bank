#!/usr/bin/env bash
# Installed as a root-owned helper. The caller is identified by sudo.
set -euo pipefail
export PATH=/usr/sbin:/usr/bin:/sbin:/bin
cd /
(( EUID == 0 && $# == 0 )) || exit 1
owner=${SUDO_USER:?Run through sudo as the Jenkins executor}
[[ $owner != root ]] && id "$owner" >/dev/null || exit 1
export DEBIAN_FRONTEND=noninteractive

# Keep the installed package family when Docker CE is already on the VM.
engine=docker.io
compose=docker-compose-v2
buildx=docker-buildx
if [[ $(dpkg-query -W -f='${Status}' docker-ce-cli 2>/dev/null || true) == 'install ok installed' ]]; then
    engine=docker-ce
    compose=docker-compose-plugin
    buildx=docker-buildx-plugin
fi

packages=()
command -v sg >/dev/null || packages+=(util-linux-extra)
command -v dockerd >/dev/null && command -v docker >/dev/null || packages+=("$engine")
docker compose version >/dev/null 2>&1 || packages+=("$compose")
docker buildx version >/dev/null 2>&1 || packages+=("$buildx")
if (( ${#packages[@]} )); then
    apt-get update
    apt-get install -y "${packages[@]}"
fi
systemctl enable --now docker.service
if ! id -nG "$owner" | tr ' ' '\n' | grep -Fx docker >/dev/null; then
    usermod -aG docker "$owner"
fi
install -d -m 0755 -o "$owner" /opt/baltic-bank/containers
