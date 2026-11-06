#!/bin/sh
set -eu
if [ "$#" -ne 2 ]; then printf '%s\n' 'Usage: sh deploy/onlyoffice/preflight-synology.sh EXISTING_COMPOSE.yml PRIVATE_ENV_FILE' >&2; exit 2; fi
base=$1
env_file=$2
root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
[ -f "$base" ] && [ -f "$env_file" ] || { printf '%s\n' 'Compose or private environment file is missing.' >&2; exit 1; }
case $(uname -m) in x86_64|amd64) ;; *) printf '%s\n' 'This pinned deployment requires an x86_64 NAS.' >&2; exit 1;; esac
if [ -r /proc/meminfo ]; then
 awk '/MemTotal:/ {printf "Host memory: %.1f GiB\n",$2/1048576; if($2<4000000) exit 1} /MemAvailable:/ {printf "Currently available memory: %.1f GiB\n",$2/1048576}' /proc/meminfo || { printf '%s\n' 'Insufficient memory for the Office minimum.' >&2; exit 1; }
fi
command -v docker >/dev/null 2>&1 || { printf '%s\n' 'Install/enable Synology Container Manager first.' >&2; exit 1; }
docker info >/dev/null 2>&1 || { printf '%s\n' 'Container Manager is stopped or this NAS user lacks Docker access.' >&2; exit 1; }
docker compose --env-file "$env_file" -f "$base" -f "$root/docker-compose.onlyoffice.synology.override.yml" config --quiet
printf '%s\n' 'Merged compose configuration is valid. Confirm these originals/backup mappings before deployment:'
if docker inspect eac-backend --format '{{range .Mounts}}{{println .Source "->" .Destination}}{{end}}'; then :; else printf '%s\n' 'Existing backend name differs; inspect its volume mappings in Container Manager.'; fi
printf '%s\n' 'Read-only preflight complete. No services or data were changed.'
