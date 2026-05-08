// PATRÓN FACTORY METHOD: Justificación técnica para evaluación parcial 2
// =============================================================
// Problema: El cliente necesita crear distintos tipos de
// componentes UI sin acoplarse a sus clases concretas.
// Solución: Definir una interfaz de creación y dejar que
// las subclases (factories) decidan qué clase instanciar.
//
// Participantes:
//   Creator (abstracto)    → ComponentFactory
//   ConcreteCreator        → ButtonFactory, CardFactory, FormFactory
//   Product (abstracto)    → interfaz implícita render()
//   ConcreteProduct        → Button, Card, Form
// =============================================================

// --- Concrete Products ---
class Button {
  render() { return `<button class="btn">Click me</button>`; }
}

class Card {
  render() { return `<div class="card">Card Content</div>`; }
}

class Form {
  render() { return `<form class="form"><input type="text" /></form>`; }
}

// --- Creator (base) ---
class ComponentFactory {
  createComponent() {
    throw new Error("createComponent() debe ser implementado por la subclase");
  }
  render() {
    const component = this.createComponent();
    return component.render();
  }
}

// --- Concrete Creators ---
class ButtonFactory extends ComponentFactory {
  createComponent() { return new Button(); }
}

class CardFactory extends ComponentFactory {
  createComponent() { return new Card(); }
}

class FormFactory extends ComponentFactory {
  createComponent() { return new Form(); }
}

// --- Client: getFactory ---
// El cliente trabaja con la factory abstracta, no con clases concretas.
// Para agregar un componente nuevo, solo se crea una nueva Factory.
function getFactory(type) {
  const factories = {
    button: new ButtonFactory(),
    card:   new CardFactory(),
    form:   new FormFactory(),
  };
  const factory = factories[type];
  if (!factory) throw new Error(`Tipo de componente desconocido: ${type}`);
  return factory;
}

module.exports = {
  ComponentFactory, ButtonFactory, CardFactory, FormFactory,
  Button, Card, Form, getFactory,
};

if (require.main === module) {
  ["button", "card", "form"].forEach((type) => {
    console.log(`Renderizando ${type}:`, getFactory(type).render());
  });
}
