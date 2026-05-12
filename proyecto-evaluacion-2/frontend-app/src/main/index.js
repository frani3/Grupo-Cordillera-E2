const express = require("express");
const { DataFacade } = require("../services/DataService");

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

// Health check
app.get("/health", (req, res) => {
  res.json({ status: "ok", service: "frontend-app", port: PORT });
});

// Proxy hacia el BFF — el frontend-app actúa de intermediario para el navegador
app.get("/api/user/:id", async (req, res) => {
  const env = process.env.NODE_ENV === "production" ? "production" : "development";
  const facade = new DataFacade(env);
  try {
    const data = await facade.getUserData(req.params.id);
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

app.get("/api/dashboard", async (req, res) => {
  const env = process.env.NODE_ENV === "production" ? "production" : "development";
  const facade = new DataFacade(env);
  try {
    const data = await facade.getDashboardData();
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: err.message });
  }
});

// Sirve página principal
app.get("/", (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html lang="es">
    <head>
      <meta charset="UTF-8">
      <title>Grupo Cordillera — Frontend</title>
      <style>body{font-family:sans-serif;max-width:800px;margin:40px auto;padding:0 20px}</style>
    </head>
    <body>
      <h1>Frontend App — Grupo Cordillera</h1>
      <p>Servicio corriendo en puerto <strong>${PORT}</strong></p>
      <ul>
        <li><a href="/health">GET /health</a></li>
        <li><a href="/api/dashboard">GET /api/dashboard</a></li>
        <li><a href="/api/user/1">GET /api/user/1</a></li>
      </ul>
    </body>
    </html>
  `);
});

app.listen(PORT, () => {
  console.log(`frontend-app escuchando en puerto ${PORT}`);
  console.log(`BFF_URL configurada: ${process.env.BFF_URL || "(no definida, se usará default del ambiente)"}`);
});

module.exports = app;
