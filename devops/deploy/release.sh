#!/usr/bin/env bash
set -euo pipefail
umask 077
revision=$1
domain=$2
root=/opt/baltic-bank
exec 9>"$root/deploy.lock"
flock -w 180 9

ready() {
    local url=$1
    shift
    for ((i=0; i<45; i++)); do
        if [[ $(curl --silent --output /dev/null --write-out '%{http_code}' \
                --connect-timeout 2 --max-time 4 "$@" "$url") == 200 ]]; then return 0; fi
        sleep 2
    done
    return 1
}

activate() {
    ln -sfnT "$1" "$root/current.next" || return 1
    mv -Tf "$root/current.next" "$root/current" || return 1
    sudo -n /usr/bin/systemctl restart baltic-bank.service
}

previous=$(readlink "$root/current" || true)
release=$(mktemp -d "$root/releases/${revision:0:12}.XXXXXXXX")
# systemd reads this file before switching to its dynamic service user.
cat > "$release/database.env"
for key in DB_URL DB_USERNAME DB_PASSWORD; do
    grep -q "^$key=" "$release/database.env" || { echo "Missing $key" >&2; exit 1; }
done
mapfile -t jars < <(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar')
[[ ${#jars[@]} == 1 && -f ${jars[0]} ]]
install -m 0644 "${jars[0]}" "$release/app.jar"
chmod 0755 "$release"
if activate "$release" && ready http://127.0.0.1:18080/login/client &&
   ready "https://$domain/login/client" --resolve "$domain:443:127.0.0.1"; then
    echo "Deployed $revision at https://$domain"
    exit 0
fi
echo 'Health check failed. Inspect journalctl -u baltic-bank.service.' >&2
if [[ -n $previous ]]; then
    activate "$previous"
    ready http://127.0.0.1:18080/login/client
else
    sudo -n /usr/bin/systemctl stop baltic-bank.service
    rm -f "$root/current"
fi
exit 1
