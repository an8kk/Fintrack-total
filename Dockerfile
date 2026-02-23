# Stage 1: Build the Flutter Web App
FROM ubuntu:20.04 AS build

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Flutter
WORKDIR /app
RUN git clone https://github.com/flutter/flutter.git -b stable
ENV PATH="/app/flutter/bin:/app/flutter/bin/cache/dart-sdk/bin:${PATH}"

# Copy source code
COPY . /app/frontend
WORKDIR /app/frontend

# Get dependencies and build
RUN flutter pub get
RUN flutter build web --release

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Install gettext for envsubst
RUN apk add --no-cache gettext

# Copy static files
COPY --from=build /app/frontend/build/web /usr/share/nginx/html

# Copy Nginx configuration template
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh

# Make entrypoint executable
RUN chmod +x /docker-entrypoint.sh

# We use the PORT environment variable provided by Render (or default 80)
EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
