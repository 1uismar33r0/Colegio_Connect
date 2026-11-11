#!/bin/sh
set -eu

# Render supplies the private Postgres URL and each credential independently.
# Only the host:port is taken from the URL; no credential-bearing URL is logged.
: "${DATABASE_URL:?Set the private Render PostgreSQL URL}"
: "${DB_NAME:?Set DB_NAME from Render PostgreSQL}"
: "${DB_USER:?Set DB_USER from Render PostgreSQL}"
: "${DB_PASSWORD:?Set DB_PASSWORD from Render PostgreSQL}"
case "$DATABASE_URL" in
    postgresql://*@*/*) ;;
    *) echo 'Invalid private PostgreSQL URL format.' >&2; exit 1 ;;
esac
endpoint=${DATABASE_URL##*@}
hostport=${endpoint%%/*}
case "$hostport" in
    *:*) ;;
    *) echo 'Private PostgreSQL URL has no host and port.' >&2; exit 1 ;;
esac
export SPRING_DATASOURCE_URL="jdbc:postgresql://${hostport}/${DB_NAME}?sslmode=${DB_SSLMODE:-require}"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
unset DATABASE_URL DB_PASSWORD

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
