export class PaymentRequest {
  constructor({
    orderId = null,
    paymentMethod = 'CASH',
    amount = 0,
  } = {}) {
    this.orderId = orderId;
    this.paymentMethod = paymentMethod;
    this.amount = Number(amount) || 0;
  }
}

export class PaymentResponse {
  constructor({
    id = null,
    orderId = null,
    paymentMethod = 'CASH',
    amount = 0,
    status = 'PENDING',
    transactionId = '',
    paymentUrl = '',
    createdAt = '',
  } = {}) {
    this.id = id;
    this.orderId = orderId;
    this.paymentMethod = paymentMethod;
    this.amount = Number(amount) || 0;
    this.status = status;
    this.transactionId = transactionId;
    this.paymentUrl = paymentUrl;
    this.createdAt = createdAt;
  }
}
