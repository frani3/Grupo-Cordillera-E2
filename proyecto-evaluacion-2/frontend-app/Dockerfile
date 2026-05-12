## ── Imagen de ejecución ───────────────────────────────────────────────────
FROM node:18-alpine
WORKDIR /app

# Instala dependencias de producción primero (cache layer)
COPY package*.json ./
RUN npm install --production

COPY src ./src

# Variables de entorno con valores por defecto seguros
ENV PORT=3000
ENV NODE_ENV=production
ENV BFF_URL=http://bff-service:8080

EXPOSE ${PORT}

CMD ["npm", "start"]
