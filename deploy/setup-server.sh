#!/usr/bin/env bash
# Puts Nami on the server that already runs MiddleBerth, at https://api.nami.samartiwari.me.
#
#   On the server, the first time:
#     git clone https://github.com/samartiwari/nami.git ~/nami && bash ~/nami/deploy/setup-server.sh
#   Every time after that:
#     bash ~/nami/deploy/setup-server.sh
#
# Safe to run again: every step checks whether it is already done. It needs
# MiddleBerth's nginx to include /srv/nginx-sites (MiddleBerth's deploy/compose.vm.yml).

set -euo pipefail

DOMAIN=api.nami.samartiwari.me
CERT_EMAIL=samartiwari2004@gmail.com
DIR="$HOME/nami"
SITES=/srv/nginx-sites
MB="$HOME/MiddleBerth"
MB_COMPOSE="docker compose --project-directory $MB -f $MB/docker-compose.yml -f $MB/deploy/compose.vm.yml"
NAMI_COMPOSE="docker compose -f $DIR/deploy/compose.yml --env-file $DIR/deploy/.env"

step() { printf '\n== %s\n' "$1"; }
fail() { printf '\n!! %s\n' "$1" >&2; exit 1; }

step "1 of 5  the code"
if [ -d "$DIR/.git" ]; then
    git -C "$DIR" pull --ff-only
else
    git clone https://github.com/samartiwari/nami.git "$DIR"
fi

step "2 of 5  the database password"
# Made here, on the server, and never written anywhere else.
if [ -f "$DIR/deploy/.env" ]; then
    echo "already there"
else
    (umask 077 && printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)" > "$DIR/deploy/.env")
    echo "generated"
fi
chmod 600 "$DIR/deploy/.env"

step "3 of 5  build and start the app and its database (the first build takes a few minutes)"
$NAMI_COMPOSE up -d --build
# Asked from inside MiddleBerth's nginx container, the way nginx will reach it.
for _ in $(seq 1 60); do
    if $MB_COMPOSE exec -T nginx wget -qO- "http://nami-app:8080/search?q=wikipedia&size=1" > /dev/null 2>&1; then
        echo "the app answers"
        break
    fi
    sleep 5
done
$MB_COMPOSE exec -T nginx wget -qO- "http://nami-app:8080/search?q=wikipedia&size=1" > /dev/null 2>&1 \
    || fail "The app did not answer within 5 minutes. See: $NAMI_COMPOSE logs app"

step "4 of 5  HTTPS certificate for $DOMAIN"
if sudo test -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem"; then
    echo "already issued; certbot renews it with the others"
else
    # Let's Encrypt checks the name points here first, and too many failed attempts
    # lock the domain out for an hour. So check before asking.
    here=$(curl -fsS --max-time 10 https://api.ipify.org || true)
    there=$(getent ahostsv4 "$DOMAIN" | awk 'NR==1 {print $1}')
    [ -n "$there" ] || fail "$DOMAIN does not resolve yet. Add its A record ($here) and run again."
    [ -z "$here" ] || [ "$here" = "$there" ] || fail "$DOMAIN points to $there, but this server is $here."
    # certbot needs port 80 for a few seconds, so nginx steps aside, as it does for renewals.
    $MB_COMPOSE stop nginx
    if ! sudo certbot certonly --standalone --non-interactive --agree-tos -m "$CERT_EMAIL" -d "$DOMAIN"; then
        $MB_COMPOSE start nginx
        fail "certbot could not get a certificate for $DOMAIN"
    fi
    $MB_COMPOSE start nginx
fi

step "5 of 5  serve $DOMAIN through MiddleBerth's nginx"
$MB_COMPOSE exec -T nginx test -d /etc/nginx/sites \
    || fail "MiddleBerth's nginx does not include $SITES yet. Update MiddleBerth and recreate its nginx first."
sudo mkdir -p "$SITES"
sudo cp "$DIR/deploy/nginx/$DOMAIN.conf" "$SITES/"
if ! $MB_COMPOSE exec -T nginx nginx -t; then
    sudo rm -f "$SITES/$DOMAIN.conf"
    fail "nginx rejected the config, so it was taken out again. MiddleBerth is unaffected."
fi
$MB_COMPOSE exec -T nginx nginx -s reload

for _ in $(seq 1 30); do
    if curl -fsS --max-time 5 --resolve "$DOMAIN:443:127.0.0.1" "https://$DOMAIN/search?q=wikipedia&size=1" > /dev/null 2>&1; then
        printf '\nNami is live at https://%s\n' "$DOMAIN"
        printf 'The crawler is building the index from scratch, about one article a second.\n'
        exit 0
    fi
    sleep 2
done
fail "Configured, but https://$DOMAIN did not answer. See: $MB_COMPOSE logs nginx"
