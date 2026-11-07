export const OrderStatus = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
};

export const OrderType = {
  DINE_IN: 'DINE_IN',
  TAKE_AWAY: 'TAKE_AWAY',
  DELIVERY: 'DELIVERY',
};

export const PaymentMethod = {
  CASH: 'CASH',
  WALLET: 'WALLET',
  BANK_TRANSFER: 'BANK_TRANSFER',
  VNPAY: 'VNPAY',
  MOMO: 'MOMO',
};

export class OrderItemResponse {
  constructor({
    id = null,
    productId = null,
    productName = '',
    quantity = 1,
    size = 'M',
    unitPrice = 0,
    totalPrice = 0,
    toppings = [],
  } = {}) {
    this.id = id;
    this.productId = productId;
    this.productName = productName;
    this.quantity = Number(quantity) || 1;
    this.size = size;
    this.unitPrice = Number(unitPrice) || 0;
    this.totalPrice = Number(totalPrice) || 0;
    this.toppings = toppings;
  }
}

export class OrderResponse {
  constructor({
    id = null,
    customerId = null,
    status = OrderStatus.PENDING,
    items = [],
    subtotal = 0,
    discountAmount = 0,
    loyaltyPointsUsed = 0,
    totalAmount = 0,
    createdAt = '',
    updatedAt = '',
    orderCode = '',
    orderType = OrderType.DINE_IN,
    tableNumber = '',
    paymentMethod = PaymentMethod.CASH,
    note = '',
  } = {}) {
    this.id = id;
    this.customerId = customerId;
    this.status = status;
    this.items = items.map((i) => new OrderItemResponse(i));
    this.subtotal = Number(subtotal) || 0;
    this.discountAmount = Number(discountAmount) || 0;
    this.loyaltyPointsUsed = Number(loyaltyPointsUsed) || 0;
    this.totalAmount = Number(totalAmount) || 0;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.orderCode = orderCode;
    this.orderType = orderType;
    this.tableNumber = tableNumber;
    this.paymentMethod = paymentMethod;
    this.note = note;
  }
}

export class CheckoutRequest {
  constructor({
    customerId = null,
    orderType = OrderType.DINE_IN,
    tableNumber = '',
    voucherCode = '',
    loyaltyPointsUsed = 0,
    note = '',
    paymentMethod = PaymentMethod.CASH,
  } = {}) {
    this.customerId = customerId;
    this.orderType = orderType;
    this.tableNumber = tableNumber;
    this.voucherCode = voucherCode;
    this.loyaltyPointsUsed = Number(loyaltyPointsUsed) || 0;
    this.note = note;
    this.paymentMethod = paymentMethod;
  }
}
