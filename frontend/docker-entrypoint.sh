#!/bin/sh
# Use envsubst to replace ${BACKEND_URL} and ${PORT} in nginx.conf.template
# Default to port 80 if not set, and backend:8080 if not set
export PORT="${PORT:-80}"
export BACKEND_URL="${BACKEND_URL:-http://backend:8080}"

echo "Starting Nginx with PORT=$PORT and BACKEND_URL=$BACKEND_URL"

envsubst '${PORT} ${BACKEND_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g "daemon off;"
