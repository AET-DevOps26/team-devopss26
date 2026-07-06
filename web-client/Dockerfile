FROM node:22-alpine AS build

WORKDIR /app

# Copy the API specifications (needed for npm run codegen)
COPY api ./api

# Copy package info and install dependencies
COPY web-client/package.json web-client/package-lock.json ./web-client/
RUN cd web-client && npm ci

# Copy the rest of the web-client source code
COPY web-client ./web-client

ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
ARG VITE_BASE_PATH
ENV VITE_BASE_PATH=$VITE_BASE_PATH

RUN cd web-client && npm run build

FROM caddy:alpine

COPY --from=build /app/web-client/dist /usr/share/caddy
COPY web-client/Caddyfile /etc/caddy/Caddyfile

EXPOSE 80

LABEL authors="w-richter"
