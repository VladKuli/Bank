#!/usr/bin/env bash
set -euo pipefail
domain=$1
email=$2
[[ $domain =~ ^([a-z0-9]([a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,63}$ ]]
[[ $email =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]
state=/etc/baltic-bank
stamp=$(sha256sum "$0" devops/deploy/{nginx-*.conf,baltic-bank.service} | sha256sum | cut -d' ' -f1)
owner=${SUDO_USER:-$(id -un)}
expected="$owner $domain $email $stamp"
if [[ -r $state/provisioned && $(cat "$state/provisioned") == "$expected" ]]; then exit 0; fi
if (( EUID != 0 )); then exec sudo -n bash "$0" "$domain" "$email"; fi
source /etc/os-release
case "$ID:$VERSION_ID" in
    ubuntu:24.04) jdk_package=temurin-25-jdk ;;
    ubuntu:26.04) jdk_package=openjdk-25-jdk-headless ;;
    *) echo 'Ubuntu Server 24.04 or 26.04 required' >&2; exit 1 ;;
esac
[[ $owner =~ ^[a-z_][a-z0-9_-]*$ && $owner != root ]]
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gpg nginx certbot python3 openssl
if [[ $jdk_package == temurin-25-jdk ]] && ! dpkg-query -W -f='${Status}' temurin-25-jdk 2>/dev/null | grep -q 'ok installed'; then
    install -d -m 0755 /etc/apt/keyrings
    curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public |
        gpg --batch --yes --dearmor -o /etc/apt/keyrings/baltic-adoptium.gpg
    chmod 0644 /etc/apt/keyrings/baltic-adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/baltic-adoptium.gpg] https://packages.adoptium.net/artifactory/deb noble main" > /etc/apt/sources.list.d/baltic-adoptium.list
    apt-get update
fi
apt-get install -y "$jdk_package"
javac=$(dpkg -L "$jdk_package" | grep '/bin/javac$')
ln -sfnT "${javac%/bin/javac}" /opt/baltic-bank-jdk
install -d -m 0755 "$state"
install -d -o "$owner" -m 0755 /opt/baltic-bank /opt/baltic-bank/releases
install -m 0644 devops/deploy/baltic-bank.service /etc/systemd/system/baltic-bank.service
printf '%s ALL=(root) NOPASSWD: /usr/bin/systemctl restart baltic-bank.service, /usr/bin/systemctl stop baltic-bank.service\n' "$owner" > "$state/sudoers.new"
chmod 0440 "$state/sudoers.new"
visudo -cf "$state/sudoers.new"
install -m 0440 "$state/sudoers.new" /etc/sudoers.d/baltic-bank
rm "$state/sudoers.new"
systemctl daemon-reload
systemctl enable baltic-bank.service
systemctl enable --now nginx
install -d -m 0755 /var/www/baltic-acme /etc/letsencrypt/renewal-hooks/deploy
site=/etc/nginx/sites-available/baltic-bank
write_site() {
    sed "s/DOMAIN_NAME/$domain/g" "$@" > "$site.new"
    mv "$site.new" "$site"
    ln -sfn "$site" /etc/nginx/sites-enabled/baltic-bank
    nginx -t
    systemctl reload nginx
}
if [[ ! -f /etc/letsencrypt/live/$domain/fullchain.pem ]]; then
    write_site devops/deploy/nginx-http.conf
    certbot certonly --non-interactive --agree-tos --email "$email" --webroot \
        -w /var/www/baltic-acme --cert-name "$domain" -d "$domain"
fi
write_site devops/deploy/nginx-{http,https}.conf
printf '#!/bin/sh\nnginx -t && systemctl reload nginx\n' > /etc/letsencrypt/renewal-hooks/deploy/baltic-bank
chmod 0755 /etc/letsencrypt/renewal-hooks/deploy/baltic-bank
systemctl enable --now certbot.timer
printf '%s\n' "$expected" > "$state/provisioned"
