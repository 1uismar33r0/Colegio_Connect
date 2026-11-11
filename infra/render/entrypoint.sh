#!/bin/sh
set -eu

. /app/parse-database-url.sh

mailpit --listen 127.0.0.1:8025 --smtp 127.0.0.1:1025 \
    --allowed-hosts localhost,127.0.0.1 --database /tmp/mailpit.db \
    --max 100 --max-age 24h --max-message-size 1 --disable-version-check --quiet &
mail_pid=$!
java -jar /app/app.jar &
java_pid=$!

shutdown() {
    trap - TERM INT
    kill -TERM "$java_pid" "$mail_pid" 2>/dev/null || true
    wait "$java_pid" 2>/dev/null || true
    wait "$mail_pid" 2>/dev/null || true
}
trap 'shutdown; exit 143' TERM INT
set +e
wait -n "$java_pid" "$mail_pid"
status=$?
set -e
shutdown
# Either child exiting without a shutdown request is a failed service.
[ "$status" -ne 0 ] || status=1
exit "$status"
