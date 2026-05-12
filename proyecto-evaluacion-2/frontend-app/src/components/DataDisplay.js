const { DataFacade } = require("../services/DataService");

// PATRÓN FACTORY METHOD: DataDisplay es el consumidor final.
// Recibe el entorno y delega la creación del servicio a DataFacade.
class DataDisplay {
  constructor(containerId, options = {}) {
    this.containerId = containerId;
    this.facade = new DataFacade(
      options.environment || "production",
      options.fetcher || null
    );
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

    return `
      <div class="data-display" id="${this._sanitize(this.containerId)}">
        <p><strong>Estado:</strong> ${this._sanitize(data.status)}</p>
        <p><strong>Datos:</strong> ${this._sanitize(data.data)}</p>
        <p><strong>Fuente:</strong> ${this._sanitize(data.source)}</p>
        <p><strong>Timestamp:</strong> ${this._sanitize(data.timestamp)}</p>
      </div>
    `.trim();
  }

  getState() {
    return { ...this.state };
  }

  _setState(partial) {
    this.state = { ...this.state, ...partial };
  }

  _sanitize(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
}

module.exports = { DataDisplay };
