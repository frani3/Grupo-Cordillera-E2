const express = require("express");
const { DataFacade } = require("../services/DataService");

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

// Crea facade pasando el fetcher con el header Authorization del request entrante
// Node 18 tiene fetch nativo (globalThis.fetch) — sin dependencias extra
function buildFacade(authToken) {
  const env = process.env.NODE_ENV === "production" ? "production" : "development";

  const authedFetch = (url, options = {}) =>
    fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        Authorization: authToken || "Bearer demo-token",
      },
    });

  return new DataFacade(env, authedFetch);
}

// Health check
app.get("/health", (req, res) => {
  res.json({ status: "ok", service: "frontend-app", port: PORT });
});

// GET /api/user/:id  →  BFF /api/proxy/data?id=user-{id}
app.get("/api/user/:id", async (req, res) => {
  const facade = buildFacade(req.headers.authorization);
  try {
    const data = await facade.getUserData(req.params.id);
    res.json(data);
  } catch (err) {
    const status = err.message.includes("401") ? 401 : 502;
    res.status(status).json({ error: err.message });
  }
});

// GET /api/dashboard  →  BFF /api/proxy/data?id=dashboard
app.get("/api/dashboard", async (req, res) => {
  const facade = buildFacade(req.headers.authorization);
  try {
    const data = await facade.getDashboardData();
    res.json(data);
  } catch (err) {
    const status = err.message.includes("401") ? 401 : 502;
    res.status(status).json({ error: err.message });
  }
});

// Página principal con instrucciones de uso
app.get("/", (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html lang="es">
    <head>
      <meta charset="UTF-8">
      <title>Grupo Cordillera — Frontend</title>
      <style>
        body { font-family: sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; }
        code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
        pre  { background: #f4f4f4; padding: 12px; border-radius: 5px; overflow-x: auto; }
      </style>
    </head>
    <body>
      <h1>Frontend App — Grupo Cordillera</h1>
      <p>Puerto: <strong>${PORT}</strong> | BFF: <strong>${process.env.BFF_URL || "http://bff-service:8080"}</strong></p>
      <h2>Endpoints disponibles</h2>
      <pre>
GET  /health                     — Estado del frontend
GET  /api/dashboard              — Datos del dashboard (requiere Authorization: Bearer)
GET  /api/user/:id               — Datos de usuario (requiere Authorization: Bearer)
      </pre>
      <h2>Ejemplo de uso</h2>
      <pre>curl http://localhost:3000/api/dashboard -H "Authorization: Bearer mi-token"</pre>
    </body>
    </html>
  `);
});

app.listen(PORT, () => {
  console.log(`frontend-app escuchando en puerto ${PORT}`);
  console.log(`BFF_URL: ${process.env.BFF_URL || "http://bff-service:8080"}`);
});

module.exports = app;
