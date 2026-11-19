# syntax=docker/dockerfile:1

FROM node:22-alpine AS assets
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY resources ./resources
COPY vite.config.js .
RUN npm run build

FROM composer:2 AS vendor
WORKDIR /app
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-interaction --prefer-dist --no-progress --no-scripts

FROM php:8.3-fpm-bookworm AS app

RUN apt-get update && apt-get install -y --no-install-recommends \
        libicu-dev libzip-dev libonig-dev default-mysql-client \
    && docker-php-ext-install -j"$(nproc)" bcmath intl mbstring opcache pdo_mysql zip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /var/www/html
COPY --from=vendor /app/vendor ./vendor
COPY . .
COPY --from=assets /app/public/build ./public/build
COPY docker/php/production.ini /usr/local/etc/php/conf.d/99-ven.ini
COPY docker/entrypoint.sh /usr/local/bin/ven-entrypoint

COPY docker/php/www.conf /usr/local/etc/php-fpm.d/zz-ven.conf

RUN php artisan package:discover --ansi

RUN chmod +x /usr/local/bin/ven-entrypoint \
    && mkdir -p storage/framework/cache storage/framework/sessions storage/framework/views bootstrap/cache \
    && chown -R www-data:www-data storage bootstrap/cache \
    && mkdir -p storage/app/public \
    && ln -sfn /var/www/html/storage/app/public /var/www/html/public/storage

EXPOSE 9000
ENTRYPOINT ["ven-entrypoint"]
CMD ["php-fpm"]

FROM nginx:1.27-alpine AS web
COPY --from=app /var/www/html/public /var/www/html/public
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
