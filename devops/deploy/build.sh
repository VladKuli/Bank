#!/usr/bin/env bash
set -euo pipefail

export BUILDER_IMAGE="baltic-bank-builder:${RELEASE_TAG:?}"
ci=(docker compose --project-name "baltic-ci-$RELEASE_TAG" -f devops/deploy/ci.yaml)
cleanup() {
    "${ci[@]}" down --volumes --remove-orphans
    docker image rm "$BUILDER_IMAGE" >/dev/null || true
}
trap cleanup EXIT

docker build --pull -f DockerFile --target build -t "$BUILDER_IMAGE" .
"${ci[@]}" run --rm tests
docker build -f DockerFile -t "baltic-bank:$RELEASE_TAG" .
