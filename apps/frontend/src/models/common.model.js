export class PageResponse {
  constructor({
    content = [],
    page = 0,
    size = 10,
    totalElements = 0,
    totalPages = 0,
  } = {}) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
  }
}

export class ApiResponse {
  constructor({
    success = true,
    message = '',
    data = null,
    errors = null,
  } = {}) {
    this.success = success;
    this.message = message;
    this.data = data;
    this.errors = errors;
  }
}
