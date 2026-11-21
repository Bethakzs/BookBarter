# Образ фронтенду BookBarter (React + Vite) із submodule bookbarter-front
# (https://github.com/hryntar/BookBarter). Dockerfile лежить в основному репозиторії,
# бо репозиторій фронту чужий і свого Dockerfile не має.
#
# Збірка (контекст — папка submodule):
#   docker build -f frontend/Dockerfile -t bookbarter-web bookbarter-front
#
# VITE_BASE_API_URL вшивається в JS під час збірки. За замовчуванням — API Gateway,
# доступний у браузері через k8s/port-forward.ps1 (http://localhost:8000).

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile --network-timeout 600000
COPY . .
ARG VITE_BASE_API_URL=http://localhost:8000
ENV VITE_BASE_API_URL=${VITE_BASE_API_URL}
RUN yarn build

FROM nginx:1.27-alpine
# SPA: усі невідомі шляхи віддають index.html (react-router)
RUN printf 'server {\n  listen 80;\n  root /usr/share/nginx/html;\n  location / {\n    try_files $uri $uri/ /index.html;\n  }\n}\n' > /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
