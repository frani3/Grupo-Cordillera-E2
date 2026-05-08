const { DataFacade } = require("../services/DataService");

// =====================================================
// Tests unitarios — DataFacade (DataService.js)
// Objetivo: cobertura > 80% en DataService.js
//
// Ramas cubiertas en constructor:
//   [1] fetcherOverride === null → overrides = {}
//   [2] fetcherOverride !== null → overrides = { fetcher }
//
// Métodos cubiertos:
//   [3] getUserData(userId) → llama fetchData("user-{userId}")
//   [4] getDashboardData()  → llama fetchData("dashboard")
//   [5] propagación de error desde el servicio
// =====================================================

const okFetch = (body) =>
  jest.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => body,
    text: async () => JSON.stringify(body),
  });

const errorFetch = (status = 500) =>
  jest.fn().mockResolvedValue({
    ok: false,
    status,
    json: async () => ({}),
    text: async () => `Error ${status}`,
  });

const SAMPLE_RESPONSE = {
  status: "ok",
  data: "datos-de-prueba",
  source: "orq-service",
  timestamp: "2026-05-08T00:00:00Z",
};

// ─────────────────────────────────────────────────────
// Constructor — rama con y sin fetcherOverride
// ─────────────────────────────────────────────────────
describe("DataFacade — constructor", () => {
  test("[1] sin fetcherOverride: crea el servicio sin fetcher personalizado", () => {
    // fetcherOverride=null → overrides={} → el servicio usa globalThis.fetch (null en Node)
    const facade = new DataFacade("development");
    expect(facade).toBeDefined();
    expect(facade.service).toBeDefined();
    // En Node.js sin fetch global, _fetcher es null; verificamos que el objeto existe
    expect(facade.service.baseUrl).toBe("http://localhost:8080");
  });

  test("[2] con fetcherOverride: el servicio recibe el fetcher personalizado", () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);
    // El fetcher debe llegar al ApiService subyacente
    expect(facade.service._fetcher).toBe(fetcher);
  });

  test("respeta el entorno pasado al constructor", () => {
    const facade = new DataFacade("production", okFetch(SAMPLE_RESPONSE));
    expect(facade.service.baseUrl).toBe("http://bff-service:8080");
  });
});

// ─────────────────────────────────────────────────────
// getUserData() — éxito, formato del requestId y error
// ─────────────────────────────────────────────────────
describe("DataFacade — getUserData()", () => {
  test("[3] retorna los datos del servidor cuando el fetch tiene éxito", async () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);

    const result = await facade.getUserData(42);

    expect(result).toEqual(SAMPLE_RESPONSE);
  });

  test("getUserData() llama al endpoint con el userId formateado como 'user-{id}'", async () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);

    await facade.getUserData(7);

    const [calledUrl] = fetcher.mock.calls[0];
    expect(calledUrl).toContain("user-7");
  });

  test("getUserData() llama exactamente una vez al fetch por invocación", async () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);

    await facade.getUserData(1);
    await facade.getUserData(2);

    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  test("[5] getUserData() propagaa el error cuando el fetch falla con HTTP 401", async () => {
    const facade = new DataFacade("test", errorFetch(401));
    await expect(facade.getUserData(1)).rejects.toThrow("HTTP 401");
  });

  test("getUserData() propagaa el error cuando el fetch falla con HTTP 500", async () => {
    const facade = new DataFacade("test", errorFetch(500));
    await expect(facade.getUserData(99)).rejects.toThrow("HTTP 500");
  });
});

// ─────────────────────────────────────────────────────
// getDashboardData() — éxito y error
// ─────────────────────────────────────────────────────
describe("DataFacade — getDashboardData()", () => {
  test("[4] retorna los datos del dashboard cuando el fetch tiene éxito", async () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);

    const result = await facade.getDashboardData();

    expect(result).toEqual(SAMPLE_RESPONSE);
  });

  test("getDashboardData() llama al endpoint con requestId 'dashboard'", async () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);

    await facade.getDashboardData();

    const [calledUrl] = fetcher.mock.calls[0];
    expect(calledUrl).toContain("dashboard");
  });

  test("getDashboardData() propaga el error del servidor", async () => {
    const facade = new DataFacade("test", errorFetch(503));
    await expect(facade.getDashboardData()).rejects.toThrow("HTTP 503");
  });
});

// ─────────────────────────────────────────────────────
// Integración ligera: getUserData + getDashboardData
// en la misma instancia (mismo fetcher, dos llamadas)
// ─────────────────────────────────────────────────────
describe("DataFacade — múltiples llamadas en la misma instancia", () => {
  test("getUserData y getDashboardData usan el mismo servicio subyacente", async () => {
    const fetcher = okFetch(SAMPLE_RESPONSE);
    const facade = new DataFacade("test", fetcher);

    const userData      = await facade.getUserData(10);
    const dashboardData = await facade.getDashboardData();

    expect(fetcher).toHaveBeenCalledTimes(2);
    expect(userData).toEqual(SAMPLE_RESPONSE);
    expect(dashboardData).toEqual(SAMPLE_RESPONSE);
  });
});
