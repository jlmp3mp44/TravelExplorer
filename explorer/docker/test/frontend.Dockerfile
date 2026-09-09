FROM node:22-alpine AS build
WORKDIR /app
COPY --from=frontend_src /package.json /package-lock.json ./
RUN npm ci
COPY --from=frontend_src /index.html /vite.config.js ./
COPY --from=frontend_src /public public
COPY --from=frontend_src /src src
ENV VITE_API_BASE_URL=""
ENV VITE_GOOGLE_MAPS_API_KEY=""
ENV VITE_DISABLE_GOOGLE_API=true
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
