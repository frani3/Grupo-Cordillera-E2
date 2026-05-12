// PATRÓN FACTORY METHOD: Justificación técnica para evaluación parcial 2
// =============================================================
// Problema: El frontend necesita instanciar clientes HTTP con
// configuraciones distintas según el entorno (dev/test/prod)
// sin que cada componente duplique condicionales de entorno.
//
// Solución: ApiServiceFactory centraliza la creación.
// El cliente llama create('data', 'production') y obtiene
// el servicio correcto sin conocer URLs ni timeouts concretos.
//
// Las URLs se leen desde variables de entorno (BFF_URL) para
// que Docker pueda configurarlas sin reconstruir la imagen.
// =============================================================

// --- Product base ---
class ApiService {
  constructor(config) {
    this.baseUrl = config.baseUrl;
    this.timeout = config.timeout;
    this.headers = { "Content-Type": "application/json", ...(config.headers || {}) };
    this._fetcher = config.fetcher || globalThis.fetch?.bind(globalThis) || null;
  }

  async get(endpoint) {
    if (!this._fetcher) throw new Error("No hay implementación de fetch disponible.");
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.timeout);
    try {
      const response = await this._fetcher(`${this.baseUrl}${endpoint}`, {
        method: "GET",
        headers: this.headers,
        signal: controller.signal,
      });
      if (!response.ok) {
        const body = await response.text();
        throw new Error(`HTTP ${response.status}: ${body}`);
      }
      return response.json();
    } finally {
      clearTimeout(timeoutId);
    }
  }
}

// --- Concrete Products ---
class DataService extends ApiService {
  async fetchData(requestId = "default") {
    return this.get(`/api/proxy/data?id=${encodeURIComponent(requestId)}`);
  }
}

class AuthService extends ApiService {
  async validateToken(token) {
    return this.get("/api/proxy/auth/validate");
  }
}

// --- Creator: ApiServiceFactory ---
class ApiServiceFactory {
  // Las URLs se resuelven en el momento de instanciar, leyendo process.env.
  // Esto permite que Docker inyecte BFF_URL sin recompilar la imagen.
  static get ENVIRONMENTS() {
    const bffUrl = (typeof process !== "undefined" && process.env.BFF_URL)
      ? process.env.BFF_URL
      : null;

    return {
      production:  { baseUrl: bffUrl || "http://bff-service:8080", timeout: 5000 },
      development: { baseUrl: bffUrl || "http://localhost:8080",   timeout: 10000 },
      test:        { baseUrl: bffUrl || "http://localhost:8080",   timeout: 1000 },
    };
  }

  static REGISTRY = {
    data: DataService,
    auth: AuthService,
  };

  // Factory Method principal
  static create(serviceType = "data", environment = "production", overrides = {}) {
    const envConfig = ApiServiceFactory.ENVIRONMENTS[environment];
    if (!envConfig) {
      throw new Error(
        `Entorno desconocido: '${environment}'. Válidos: ${Object.keys(ApiServiceFactory.ENVIRONMENTS).join(", ")}`
      );
    }
    const ServiceClass = ApiServiceFactory.REGISTRY[serviceType];
    if (!ServiceClass) {
      throw new Error(
        `Tipo desconocido: '${serviceType}'. Válidos: ${Object.keys(ApiServiceFactory.REGISTRY).join(", ")}`
      );
    }
    return new ServiceClass({ ...envConfig, ...overrides });
  }

  // Extensión sin modificar (principio OCP)
  static register(serviceType, ServiceClass) {
    if (typeof ServiceClass !== "function") throw new TypeError("ServiceClass debe ser un constructor");
    ApiServiceFactory.REGISTRY[serviceType] = ServiceClass;
  }
}

module.exports = { ApiServiceFactory, ApiService, DataService, AuthService };
