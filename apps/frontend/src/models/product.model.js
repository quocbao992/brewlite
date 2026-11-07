export class ProductResponse {
  constructor({
    id = null,
    categoryId = null,
    categoryName = '',
    name = '',
    imgURL = '',
    stock = 0,
    unitPrice = 0,
    status = true,
  } = {}) {
    this.id = id;
    this.categoryId = categoryId;
    this.categoryName = categoryName;
    this.name = name;
    this.imgURL = imgURL;
    this.stock = stock;
    this.unitPrice = Number(unitPrice) || 0;
    this.status = Boolean(status);
  }
}

export class ProductRequest {
  constructor({
    name = '',
    categoryId = null,
    imgURL = '',
    unitPrice = 0,
    stock = 0,
    status = true,
  } = {}) {
    this.name = name;
    this.categoryId = categoryId;
    this.imgURL = imgURL;
    this.unitPrice = Number(unitPrice) || 0;
    this.stock = Number(stock) || 0;
    this.status = Boolean(status);
  }
}
