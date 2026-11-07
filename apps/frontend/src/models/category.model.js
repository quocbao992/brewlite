export class CategoryResponse {
  constructor({
    id = null,
    name = '',
    status = true,
    displayOrder = 0,
  } = {}) {
    this.id = id;
    this.name = name;
    this.status = Boolean(status);
    this.displayOrder = Number(displayOrder) || 0;
  }
}

export class CategoryRequest {
  constructor({
    name = '',
    status = true,
    displayOrder = 0,
  } = {}) {
    this.name = name;
    this.status = Boolean(status);
    this.displayOrder = Number(displayOrder) || 0;
  }
}
