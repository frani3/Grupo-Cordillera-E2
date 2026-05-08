const { DataDisplay } = require("../components/DataDisplay");

// =====================================================
// Tests unitarios — DataDisplay
// Objetivo: cobertura > 80% en DataDisplay.js
//
// Ramas cubiertas en render():
//   [1] loading === true
//   [2] error !== null
//   [3] data === null (estado inicial)
//   [4] data cargado → HTML completo
//
// Ramas cubiertas en loadUserData():
//   [5] resolución exitosa del fetch
//   [6] rechazo del fetch (error)
//
// Ramas cubiertas en constructor:
//   [7] sin options → environment "production", fetcher null
//   [8] con options.environment y options.fetcher explícitos
//
// _sanitize():
//   [9]  & → &amp;
//   [10] < → &lt;
//   [11] > → &gt;
//   [12] " → &quot;
// =====================================================

// Helper: crea un fetch mock que resuelve con el cuerpo indicado
const okFetch = (body) =>
  jest.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => body,
    text: async () => JSON.stringify(body),
  });

// Helper: crea un fetch mock que responde con error HTTP
const errorFetch = (status = 401) =>
  jest.fn().mockResolvedValue({
    ok: false,
    status,
    json: async () => ({}),
    text: async () => "Unauthorized",
  });

const SAMPLE_DATA = {
  status: "ok",
  data: "resultado-del-servidor",
  source: "orq-service",
  timestamp: "2026-05-08T00:00:00Z",
};

// ─────────────────────────────────────────────────────
// Constructor
// ─────────────────────────────────────────────────────
describe("DataDisplay — constructor", () => {
  test("[7] sin options usa 'production' como entorno por defecto", () => {
    // No hace HTTP, solo verifica que el objeto se crea sin error
    const display = new DataDisplay("container-id");
    expect(display.containerId).toBe("container-id");
    expect(display.getState()).toEqual({ loading: false, data: null, error: null });
  });

  test("[8] con options.environment y options.fetcher explícitos", () => {
    const fetcher = okFetch(SAMPLE_DATA);
    const display = new DataDisplay("c", { environment: "test", fetcher });
    expect(display.containerId).toBe("c");
    // El fetcher se pasa al servicio subyacente; verificamos el estado inicial
    expect(display.getState().loading).toBe(false);
  });
});

// ─────────────────────────────────────────────────────
// render() — cuatro ramas
// ─────────────────────────────────────────────────────
describe("DataDisplay — render()", () => {
  test("[3] estado inicial retorna 'Sin datos disponibles'", () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(SAMPLE_DATA) });
    expect(display.render()).toContain("Sin datos disponibles");
  });

  test("[1] estado loading retorna indicador de carga", async () => {
    // Creamos un fetcher que nunca resuelve mientras el test lo necesita
    let resolveHold;
    const holdFetcher = jest.fn().mockReturnValue(
      new Promise((res) => { resolveHold = res; })
    );
    const display = new DataDisplay("c", { environment: "test", fetcher: holdFetcher });

    // Iniciamos sin await → queda en estado loading
    const loadPromise = display.loadUserData(1);
    expect(display.render()).toContain("Cargando datos...");

    // Resolvemos para no dejar la promesa pendiente (limpieza)
    resolveHold({
      ok: true,
      status: 200,
      json: async () => SAMPLE_DATA,
      text: async () => "",
    });
    await loadPromise;
  });

  test("[2] estado error retorna mensaje de error", async () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: errorFetch(500) });

    await expect(display.loadUserData(1)).rejects.toThrow();

    const html = display.render();
    expect(html).toContain("Error:");
    expect(display.getState().error).toBeTruthy();
  });

  test("[4] estado con datos retorna HTML completo con los valores", async () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(SAMPLE_DATA) });
    await display.loadUserData(99);

    const html = display.render();
    expect(html).toContain("resultado-del-servidor");
    expect(html).toContain("orq-service");
    expect(html).toContain("ok");
  });
});

// ─────────────────────────────────────────────────────
// loadUserData() — ramas de éxito y error
// ─────────────────────────────────────────────────────
describe("DataDisplay — loadUserData()", () => {
  test("[5] éxito: actualiza state.data y limpia loading y error", async () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(SAMPLE_DATA) });
    const result = await display.loadUserData(42);

    const state = display.getState();
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
    expect(state.data).toEqual(SAMPLE_DATA);
    expect(result).toEqual(SAMPLE_DATA);
  });

  test("[6] error: actualiza state.error, limpia loading, y re-lanza la excepción", async () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: errorFetch(403) });

    await expect(display.loadUserData(1)).rejects.toThrow("HTTP 403");

    const state = display.getState();
    expect(state.loading).toBe(false);
    expect(state.error).toMatch(/HTTP 403/);
    expect(state.data).toBeNull();
  });

  test("getState() retorna una copia del estado (no la referencia interna)", async () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(SAMPLE_DATA) });
    await display.loadUserData(1);

    const state = display.getState();
    // Mutar la copia no debe afectar el estado interno
    state.data = null;
    expect(display.getState().data).toEqual(SAMPLE_DATA);
  });
});

// ─────────────────────────────────────────────────────
// _sanitize() — los 4 caracteres especiales HTML
// ─────────────────────────────────────────────────────
describe("DataDisplay — _sanitize() vía render() con payload XSS", () => {
  const xssData = {
    status: "ok",
    data: '<script>&"payload"</script>',   // contiene <, >, &, "
    source: "test-source",
    timestamp: "t",
  };

  test("[9-12] escapa &, <, > y \" en datos provenientes del servidor", async () => {
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(xssData) });
    await display.loadUserData(1);

    const html = display.render();
    // Ningún carácter peligroso debe aparecer sin escapar
    expect(html).not.toContain("<script>");
    expect(html).not.toContain("</script>");
    // Los escapes deben estar presentes
    expect(html).toContain("&lt;script&gt;");
    expect(html).toContain("&amp;");
    expect(html).toContain("&quot;");
  });

  test("_sanitize escapa '&' correctamente (primer reemplazo)", async () => {
    const data = { ...SAMPLE_DATA, data: "a & b" };
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(data) });
    await display.loadUserData(1);
    expect(display.render()).toContain("a &amp; b");
  });

  test("_sanitize escapa '<' y '>' correctamente", async () => {
    const data = { ...SAMPLE_DATA, data: "<valor>" };
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(data) });
    await display.loadUserData(1);
    const html = display.render();
    expect(html).toContain("&lt;valor&gt;");
  });

  test("_sanitize escapa '\"' correctamente", async () => {
    const data = { ...SAMPLE_DATA, data: 'dato "entre comillas"' };
    const display = new DataDisplay("c", { environment: "test", fetcher: okFetch(data) });
    await display.loadUserData(1);
    expect(display.render()).toContain("&quot;");
  });
});
