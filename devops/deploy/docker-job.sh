#!/usr/bin/env bash
set -euo pipefail
export DOCKER_HOST=unix:///var/run/docker.sock
unset DOCKER_CONTEXT

# sg applies new group membership to this step without restarting Jenkins.
docker_command() {
    local command=$1
    if docker info >/dev/null 2>&1; then
        bash -c "$command"
    elif id -nG "$(id -un)" | tr ' ' '\n' | grep -Fx docker >/dev/null; then
        sg docker -c "$command"
    else
        return 1
    fi
}

ready() {
    [[ -w /opt/baltic-bank/containers && -x /opt/baltic-bank/containers ]] &&
        systemctl is-enabled --quiet docker.service &&
        docker compose version >/dev/null 2>&1 &&
        docker buildx version >/dev/null 2>&1 &&
        docker_command 'docker info >/dev/null' </dev/null
}

case "${1:-}" in
    prepare)
        if ! ready; then
            helper=/usr/local/sbin/baltic-bank-prepare-docker
            if [[ -x $helper ]] && sudo -n -l "$helper" >/dev/null 2>&1; then
                sudo -n "$helper"
            else
                echo "Docker preparation needs host privileges. Once, as root in the updated checkout, run: bash devops/deploy/authorize-docker.sh $(id -un)" >&2
                exit 1
            fi
            ready || { echo 'The executor cannot use local Docker, Compose, Buildx or the deployment directory.' >&2; exit 1; }
        fi
        docker_command 'docker version --format "{{.Server.Version}}" && docker compose version && docker buildx version'
        ;;
    build) docker_command 'exec bash devops/deploy/build.sh' ;;
    deploy) docker_command 'exec bash devops/deploy/release.sh' ;;
    *) echo 'Usage: docker-job.sh prepare|build|deploy' >&2; exit 2 ;;
esac
