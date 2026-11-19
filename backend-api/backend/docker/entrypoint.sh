#!/bin/sh
set -eu

mkdir -p storage/framework/cache/data storage/framework/sessions storage/framework/views bootstrap/cache
if [ "$(id -u)" = "0" ]; then
    chown -R www-data:www-data storage bootstrap/cache
fi

if [ "${VEN_RUN_MIGRATIONS:-false}" = "true" ]; then
    php artisan package:discover --ansi
    php artisan ven:setup-db --no-interaction
    php artisan migrate --database=mysql_dba --force
    # The first setup runs before tables exist; repeat to apply table-level grants.
    php artisan ven:setup-db --no-interaction
    php artisan optimize
fi

exec "$@"
