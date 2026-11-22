#!/usr/bin/env bash
# Runs as the unprivileged deployment user in the extracted checkout.
set -euo pipefail
umask 077
export JAVA_HOME=/opt/baltic-bank-jdk
export PATH="$JAVA_HOME/bin:$PATH"
pg_bin=$(find /usr/lib/postgresql -mindepth 3 -maxdepth 3 -name initdb | sort -V | tail -1)
[[ -x $pg_bin ]] || { echo 'Existing PostgreSQL server binaries required in /usr/lib/postgresql' >&2; exit 1; }
pg_bin=$(dirname "$pg_bin")
test_dir=$(mktemp -d /tmp/baltic-test.XXXXXXXX)
cleanup() {
    if [[ -f $test_dir/data/postmaster.pid ]]; then
        "$pg_bin/pg_ctl" -D "$test_dir/data" -m fast -w stop
    fi
    rm -rf -- "$test_dir"
}
trap cleanup EXIT
export DB_USERNAME=bank_test
DB_PASSWORD=$(openssl rand -hex 32)
export DB_PASSWORD
printf '%s\n' "$DB_PASSWORD" > "$test_dir/password"
"$pg_bin/initdb" -D "$test_dir/data" --no-locale -E UTF8 -U "$DB_USERNAME" \
    --auth=scram-sha-256 --pwfile="$test_dir/password" >/dev/null
port=$(python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()')
"$pg_bin/pg_ctl" -D "$test_dir/data" -l "$test_dir/postgres.log" \
    -o "-h 127.0.0.1 -p $port -k $test_dir" -w start
export DB_URL="jdbc:postgresql://127.0.0.1:$port/postgres"
bash gradlew --no-daemon clean test bootJar
