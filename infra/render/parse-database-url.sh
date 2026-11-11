#!/bin/sh
# Sourced by entrypoint.sh. Use Render's separately supplied user/password;
# never log or pass the credential-bearing URL to the JDBC driver.
: "${DATABASE_URL:?Set the private Render PostgreSQL URL}"
: "${DB_NAME:?Set DB_NAME from Render PostgreSQL}"
: "${DB_USER:?Set DB_USER from Render PostgreSQL}"
: "${DB_PASSWORD:?Set DB_PASSWORD from Render PostgreSQL}"

invalid_database_url() {
    echo "Private PostgreSQL URL has invalid $1." >&2
    exit 1
}

case "$DATABASE_URL" in
    postgresql://*) endpoint=${DATABASE_URL#postgresql://} ;;
    postgres://*) endpoint=${DATABASE_URL#postgres://} ;;
    *) invalid_database_url scheme ;;
esac
case "$endpoint" in
    */*) authority=${endpoint%%/*}; path=${endpoint#*/} ;;
    *) invalid_database_url database ;;
esac
case "$authority" in
    *@*) hostport=${authority##*@} ;;
    *) invalid_database_url credentials ;;
esac

url_database=${path%%\?*}
case "$url_database" in
    ''|*/*|*#*|*[!A-Za-z0-9._-]*) invalid_database_url database ;;
esac
case "$DB_NAME" in
    ''|*[!A-Za-z0-9._-]*) invalid_database_url database ;;
esac
[ "$url_database" = "$DB_NAME" ] || invalid_database_url database

case "$hostport" in
    \[*\]:*) host=${hostport%:*}; port=${hostport##*:} ;;
    \[*\]) host=$hostport; port=5432 ;;
    *:*) host=${hostport%:*}; port=${hostport##*:} ;;
    *) host=$hostport; port=5432 ;;
esac
case "$host" in
    \[*\])
        address=${host#\[}; address=${address%\]}
        case "$address" in ''|*[!0-9A-Fa-f:.]*) invalid_database_url host ;; esac
        ;;
    ''|*[!A-Za-z0-9._-]*) invalid_database_url host ;;
esac
case "$port" in
    ''|*[!0-9]*|??????*) invalid_database_url port ;;
esac
[ "$port" -ge 1 ] && [ "$port" -le 65535 ] || invalid_database_url port

export SPRING_DATASOURCE_URL="jdbc:postgresql://${host}:${port}/${DB_NAME}?sslmode=${DB_SSLMODE:-require}"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
unset DATABASE_URL DB_PASSWORD
