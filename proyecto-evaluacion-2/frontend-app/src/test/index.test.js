const {
  ButtonFactory, CardFactory, FormFactory,
  Button, Card, Form, getFactory,
} = require("../main/index");

// =====================================================
// Tests unitarios — Patrón Factory Method (index.js)
// =====================================================

describe("Concrete Products", () => {
  test("Button.render() retorna HTML con etiqueta <button>", () => {
    expect(new Button().render()).toContain("<button");
    expect(new Button().render()).toContain("</button>");
  });
  test("Card.render() retorna HTML con clase card", () => {
    expect(new Card().render()).toContain("card");
  });
  test("Form.render() retorna HTML con etiqueta <form>", () => {
    expect(new Form().render()).toContain("<form");
    expect(new Form().render()).toContain("</form>");
  });
});

describe("Concrete Factories", () => {
  test("ButtonFactory crea instancia de Button", () => {
    expect(new ButtonFactory().createComponent()).toBeInstanceOf(Button);
  });
  test("CardFactory crea instancia de Card", () => {
    expect(new CardFactory().createComponent()).toBeInstanceOf(Card);
  });
  test("FormFactory crea instancia de Form", () => {
    expect(new FormFactory().createComponent()).toBeInstanceOf(Form);
  });
  test("ButtonFactory.render() delega al producto", () => {
    expect(new ButtonFactory().render()).toContain("<button");
  });
});

describe("Client — getFactory()", () => {
  test("getFactory('button') retorna ButtonFactory", () => {
    expect(getFactory("button")).toBeInstanceOf(ButtonFactory);
  });
  test("getFactory('card') retorna CardFactory", () => {
    expect(getFactory("card")).toBeInstanceOf(CardFactory);
  });
  test("getFactory('form') retorna FormFactory", () => {
    expect(getFactory("form")).toBeInstanceOf(FormFactory);
  });
  test("getFactory con tipo desconocido lanza Error", () => {
    expect(() => getFactory("unknown")).toThrow("Tipo de componente desconocido: unknown");
  });
  test("todos los tipos renderizan HTML válido", () => {
    ["button", "card", "form"].forEach((t) => {
      const html = getFactory(t).render();
      expect(typeof html).toBe("string");
      expect(html.length).toBeGreaterThan(0);
    });
  });
});
