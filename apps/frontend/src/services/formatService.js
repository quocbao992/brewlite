export const formatService = {
  formatMoney(amount) {
    const num = Number(amount) || 0;
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(num);
  },

  formatCurrency(amount) {
    const num = Number(amount) || 0;
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(num);
  },

  parseDate(dateInput) {
    if (!dateInput) return null;
    if (dateInput instanceof Date) return dateInput;
    if (typeof dateInput === 'number') return new Date(dateInput);
    const str = String(dateInput).trim();
    if (!str) return null;
    const hasTz = /[zZ]$|[+-]\d{2}(?::?\d{2})?$/.test(str);
    const date = new Date(hasTz ? str : `${str}Z`);
    return isNaN(date.getTime()) ? new Date(str) : date;
  },

  getOrderRemainingSeconds(createdAt, now = Date.now()) {
    if (!createdAt) return 0;
    const str = String(createdAt).trim();
    if (!str) return 0;

    const parsedUtc = new Date(str.endsWith('Z') ? str : `${str}Z`).getTime();
    const parsedLocal = new Date(str.replace(/Z$/, '')).getTime();

    const diffUtc = Math.floor((now - parsedUtc) / 1000);
    const diffLocal = Math.floor((now - parsedLocal) / 1000);

    let actualElapsed = Math.min(
      diffUtc >= 0 ? diffUtc : Infinity,
      diffLocal >= 0 ? diffLocal : Infinity
    );

    if (!isFinite(actualElapsed)) {
      actualElapsed = Math.min(Math.abs(diffUtc), Math.abs(diffLocal));
    }
    if (isNaN(actualElapsed) || actualElapsed < 0) {
      actualElapsed = 0;
    }

    return Math.max(0, 300 - actualElapsed);
  },

  formatDateTime(dateStr) {
    if (!dateStr) return '';
    try {
      const date = this.parseDate(dateStr);
      if (!date || isNaN(date.getTime())) return String(dateStr);
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(date);
    } catch {
      return String(dateStr);
    }
  },

  getOrderStatusInfo(status) {
    switch (status) {
      case 'PENDING':
        return { label: 'Chờ thanh toán', color: '#b45309', bg: '#fef3c7' };
      case 'CONFIRMED':
        return { label: 'Đã xác nhận', color: '#2563eb', bg: '#eff6ff' };
      case 'PAID':
        return { label: 'Đã thanh toán', color: '#0284c7', bg: '#e0f2fe' };
      case 'PREPARING':
      case 'PROCESSING':
        return { label: 'Đang pha chế', color: '#7c3aed', bg: '#f5f3ff' };
      case 'READY':
        return { label: 'Sẵn sàng lấy', color: '#16a34a', bg: '#dcfce7' };
      case 'COMPLETED':
        return { label: 'Hoàn thành', color: '#16a34a', bg: '#dcfce7' };
      case 'PAYMENT_FAILED':
      case 'CANCELLED':
        return { label: 'Đã hủy', color: '#dc2626', bg: '#fee2e2' };
      default:
        return { label: status || 'Không rõ', color: '#64748b', bg: '#f1f5f9' };
    }
  },

  formatPaymentMethod(method) {
    if (!method) return 'Đang xử lý';
    const m = String(method).toUpperCase();
    if (m.includes('VNPAY')) return 'Chuyển khoản QR (VNPay)';
    if (m.includes('MOMO')) return 'Ví MoMo';
    if (m.includes('STRIPE')) return 'Thẻ quốc tế (Stripe)';
    return method;
  },

  getToppingAvailability(topping) {
    if (!topping) {
      return { isOutOfStock: false, isDiscontinued: false, isDisabled: false, label: null };
    }
    const isDiscontinued = topping.status === false;
    const isOutOfStock = topping.stock !== undefined && topping.stock !== null && Number(topping.stock) <= 0;
    const isDisabled = isDiscontinued || isOutOfStock;
    const label = isOutOfStock ? 'Hết hàng' : isDiscontinued ? 'Ngừng bán' : null;
    return { isOutOfStock, isDiscontinued, isDisabled, label };
  },

  buildToppingMap(toppingRes) {
    const list = Array.isArray(toppingRes) ? toppingRes : toppingRes?.data || [];
    const map = {};
    list.forEach((tp) => {
      if (tp?.id != null) {
        map[tp.id] = tp;
      }
    });
    return map;
  },

  getUnavailableToppings(itemToppings, toppingMap = {}) {
    if (!Array.isArray(itemToppings) || itemToppings.length === 0) return [];
    return itemToppings
      .map((t) => {
        const id = t.id || t.toppingId;
        const live = id != null && toppingMap[id] ? toppingMap[id] : t;
        const availability = this.getToppingAvailability(live);
        if (!availability.isDisabled) return null;
        return {
          id,
          name: live.name || t.name,
          ...availability,
        };
      })
      .filter(Boolean);
  },

  getPaymentChannelInfo(method) {
    const m = String(method || '').toUpperCase();
    if (m.includes('MOMO')) {
      return {
        bankName: 'Ví điện tử MoMo',
        accountNumber: '0987654321',
        accountName: 'BrewLite Coffee & Tea',
        headerColor: '#a50064',
      };
    }
    if (m.includes('STRIPE')) {
      return {
        bankName: 'Cổng quốc tế Stripe',
        accountNumber: 'STRIPE-BREWLITE',
        accountName: 'BrewLite Coffee & Tea',
        headerColor: '#4f46e5',
      };
    }
    return {
      bankName: 'VietinBank',
      accountNumber: '102874928192',
      accountName: 'BrewLite Coffee & Tea',
      headerColor: '#1e4675',
    };
  },

  buildGatewayUrl({ orderId, orderCode, amount, method = 'VNPay', ref }) {
    const origin = typeof window !== 'undefined' ? window.location.origin : '';
    const params = new URLSearchParams();
    if (orderId != null) params.set('orderId', String(orderId));
    if (orderCode) params.set('orderCode', String(orderCode));
    if (amount != null) params.set('amount', String(amount));
    if (method) params.set('method', String(method));
    if (ref) params.set('ref', String(ref));
    return `${origin}/mock-gateway?${params.toString()}`;
  },
};

export default formatService;
