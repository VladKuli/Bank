#!/usr/bin/env bash
set -euo pipefail
domain=balticbank.duckdns.org
email=goldydefault@gmail.com
(( EUID == 0 )) || { echo 'Run as root from the checkout.' >&2; exit 1; }
curl -fsS --max-time 5 http://127.0.0.1:18081/login/client >/dev/null
site=/etc/nginx/sites-available/baltic-bank
if [[ -f $site && ! -f $site.pre-container ]]; then cp -p "$site" "$site.pre-container"; fi
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
curl -fsS --max-time 10 --resolve "$domain:443:127.0.0.1" "https://$domain/login/client" >/dev/null
touch /opt/baltic-bank/containers/nginx-active
