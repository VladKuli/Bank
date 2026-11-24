#!/usr/bin/env bash
set -euo pipefail

root=/opt/baltic-bank/containers
export APP_IMAGE="baltic-bank:${RELEASE_TAG:?}"
compose=(docker compose -f "$root/compose.yaml")
exec 9>"$root/deploy.lock"
flock -w 180 9

install -m 0644 devops/deploy/compose.yaml "$root/compose.next"
mv -f "$root/compose.next" "$root/compose.yaml"
docker image inspect "$APP_IMAGE" >/dev/null
container=$("${compose[@]}" ps -aq app)
previous=''
if [[ -n $container ]]; then
    previous=$(docker inspect --format '{{.Config.Image}}' "$container")
fi

if "${compose[@]}" up -d --wait --wait-timeout 150 &&
   curl -fsS --max-time 5 http://127.0.0.1:18081/login/client >/dev/null; then
    if [[ ! -f $root/nginx-active ]]; then
        echo 'Container ready on 127.0.0.1:18081. Activate its Nginx site once to direct public traffic here.'
        exit 0
    fi
    if curl -fsS --max-time 10 --resolve "$BANK_DOMAIN:443:127.0.0.1" "https://$BANK_DOMAIN/login/client" >/dev/null; then
        echo "Deployed $APP_IMAGE"
        exit 0
    fi
fi

echo 'Release verification failed.' >&2
if [[ -n $previous && $previous != "$APP_IMAGE" ]]; then
    export APP_IMAGE=$previous
    "${compose[@]}" up -d --wait --wait-timeout 150
elif [[ -z $previous ]]; then
    "${compose[@]}" stop app
fi
exit 1
