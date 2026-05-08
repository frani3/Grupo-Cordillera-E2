const { ApiServiceFactory, ApiService, DataService, AuthService } = require("../services/ApiServiceFactory");

// =====================================================
// Tests unitarios — ApiServiceFactory (Factory Method)
// =====================================================

const mockFetch = (status = 200, body = {}) =>
  jest.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => JSON.stringify(body),
  });

describe("ApiServiceFactory — Creación de servicios", () => {
  test("create('data') produce DataService", () => {
    expect(ApiServiceFactory.create("data", "development")).toBeInstanceOf(DataService);
  });
  test("create('auth') produce AuthService", () => {
    expect(ApiServiceFactory.create("auth", "development")).toBeInstanceOf(AuthService);
  });
  test("todos los productos son instancias de ApiService", () => {
    expect(ApiServiceFactory.create("data", "development")).toBeInstanceOf(ApiService);
  });
});

describe("ApiServiceFactory — Configuración por entorno", () => {
  test("production usa baseUrl del servidor docker", () => {
    expect(ApiServiceFactory.create("data", "production").baseUrl).toBe("http://bff-service:8080");
  });
  test("development usa localhost", () => {
    expect(ApiServiceFactory.create("data", "development").baseUrl).toBe("http://localhost:8080");
  });
  test("test tiene timeout menor que production", () => {
    const prod = ApiServiceFactory.create("data", "production");
    const test = ApiServiceFactory.create("data", "test");
    expect(prod.timeout).toBeGreaterThan(test.timeout);
  });
});

describe("ApiServiceFactory — Manejo de errores", () => {
  test("tipo desconocido lanza Error descriptivo", () => {
    expect(() => ApiServiceFactory.create("unknown", "production"))
      .toThrow("Tipo desconocido: 'unknown'");
  });
  test("entorno desconocido lanza Error descriptivo", () => {
    expect(() => ApiServiceFactory.create("data", "staging"))
      .toThrow("Entorno desconocido: 'staging'");
  });
});

describe("ApiServiceFactory — Extensión con register() (OCP)", () => {
  class ReportService extends ApiService {}

  test("register() agrega nuevo tipo sin modificar la factory", () => {
    ApiServiceFactory.register("report", ReportService);
    expect(ApiServiceFactory.create("report", "development")).toBeInstanceOf(ReportService);
  });
  test("register() con valor no-constructor lanza TypeError", () => {
    expect(() => ApiServiceFactory.register("bad", "no-es-clase")).toThrow(TypeError);
  });
});

describe("DataService — Llamadas HTTP con fetcher mock", () => {
  test("fetchData() llama al endpoint correcto", async () => {
    const responseBody = { status: "ok", data: "payload", source: "orq-service", timestamp: "t" };
    const fetcher = mockFetch(200, responseBody);
    const service = ApiServiceFactory.create("data", "test", { fetcher });

    const result = await service.fetchData("user-42");

    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(fetcher.mock.calls[0][0]).toContain("/api/proxy/data");
    expect(fetcher.mock.calls[0][0]).toContain("user-42");
    expect(result).toEqual(responseBody);
  });

  test("fetchData() lanza Error cuando el servidor responde 401", async () => {
    const service = ApiServiceFactory.create("data", "test", { fetcher: mockFetch(401) });
    await expect(service.fetchData("req")).rejects.toThrow("HTTP 401");
  });
});
