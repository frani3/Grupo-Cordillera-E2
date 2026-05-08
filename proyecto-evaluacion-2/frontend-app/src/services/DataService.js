// PATRÓN FACTORY METHOD: Justificación técnica para evaluación parcial 2
// DataFacade es el "código cliente" del patrón: usa ApiServiceFactory
// sin acoplarse a DataService directamente → principio DIP.

const { ApiServiceFactory } = require("./ApiServiceFactory");

class DataFacade {
  constructor(environment = "production", fetcherOverride = null) {
    const overrides = fetcherOverride ? { fetcher: fetcherOverride } : {};
    this.service = ApiServiceFactory.create("data", environment, overrides);
  }

  async getUserData(userId) {
    return this.service.fetchData(`user-${userId}`);
  }

  async getDashboardData() {
    return this.service.fetchData("dashboard");
  }
}

module.exports = { DataFacade };
