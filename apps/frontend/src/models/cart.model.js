export const ProductSize = {
  S: 'S',
  M: 'M',
  L: 'L',
};

export class CartItemToppingResponse {
  constructor({
    id = null,
    toppingId = null,
    name = '',
    price = 0,
  } = {}) {
    this.id = id;
    this.toppingId = toppingId;
    this.name = name;
    this.price = Number(price) || 0;
  }
}

export class CartItemResponse {
  constructor({
    id = null,
    productId = null,
    name = '',
    img = '',
    quantity = 1,
    size = ProductSize.M,
    unitPrice = 0,
    note = '',
    toppings = [],
    itemTotalPrice = 0,
  } = {}) {
    this.id = id;
    this.productId = productId;
    this.name = name;
    this.img = img;
    this.quantity = Number(quantity) || 1;
    this.size = size;
    this.unitPrice = Number(unitPrice) || 0;
    this.note = note;
    this.toppings = toppings.map((t) => new CartItemToppingResponse(t));
    this.itemTotalPrice = Number(itemTotalPrice) || 0;
  }
}

export class CartResponse {
  constructor({
    id = null,
    customerId = null,
    items = [],
    totalPrice = 0,
    totalItems = 0,
  } = {}) {
    this.id = id;
    this.customerId = customerId;
    this.items = items.map((i) => new CartItemResponse(i));
    this.totalPrice = Number(totalPrice) || 0;
    this.totalItems = Number(totalItems) || items.length;
  }
}

export class CartItemRequest {
  constructor({
    productId = null,
    quantity = 1,
    size = ProductSize.M,
    note = '',
    toppingIds = [],
  } = {}) {
    this.productId = productId;
    this.quantity = Number(quantity) || 1;
    this.size = size;
    this.note = note;
    this.toppingIds = toppingIds;
  }
}
