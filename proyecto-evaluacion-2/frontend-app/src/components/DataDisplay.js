// PATRÓN FACTORY METHOD: Justificación técnica para evaluación parcial 2
// Componente UI reutilizable (módulo NPM). No conoce URLs ni clases HTTP:
// delega a DataFacade que usa la Factory. Principio SRP aplicado.

const { DataFacade } = require("../services/DataService");

class DataDisplay {
  constructor(containerId, options = {}) {
    this.containerId = containerId;
    this.facade = new DataFacade(options.environment || "production", options.fetcher || null);
    this.state = { loading: false, data: null, error: null };
  }

  async loadUserData(userId) {
    this._setState({ loading: true, error: null });
    try {
      const data = await this.facade.getUserData(userId);
      this._setState({ loading: false, data });
      return data;
    } catch (error) {
      this._setState({ loading: false, error: error.message });
      throw error;
    }
  }

  render() {
    const { loading, data, error } = this.state;
    if (loading) return `<div class="data-display loading">Cargando datos...</div>`;
    if (error)   return `<div class="data-display error">Error: ${this._sanitize(error)}</div>`;
    if (!data)   return `<div class="data-display empty">Sin datos disponibles</div>`;

    return `<div class="data-display">
  <span class="status">${this._sanitize(data.status)}</span>
  <p>${this._sanitize(String(data.data))}</p>
  <small>Fuente: ${this._sanitize(data.source)}</small>
</div>`.trim();
  }

  getState() { return { ...this.state }; }
  _setState(partial) { this.state = { ...this.state, ...partial }; }

  // Previene XSS al insertar datos externos en el DOM
  _sanitize(value) {
    return String(value)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;")
      .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }
}

module.exports = { DataDisplay };
