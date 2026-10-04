#!/bin/sh
set -eu

cd "$(dirname "$0")"
for name in mysql_root_password mysql_app_password jwt_secret ai_api_key embedding_api_key; do
    if [ -e "secrets/$name" ]; then
        echo "secrets/$name already exists; refusing to overwrite" >&2
        exit 1
    fi
done

umask 077
mkdir -p secrets
openssl rand -hex 32 > secrets/mysql_root_password
openssl rand -hex 32 > secrets/mysql_app_password
openssl rand -hex 48 > secrets/jwt_secret
: > secrets/ai_api_key
: > secrets/embedding_api_key
echo 'Secret files created. AI keys are intentionally blank.'
