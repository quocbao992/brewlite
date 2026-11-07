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

  formatDateTime(dateStr) {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(date);
    } catch {
      return dateStr;
    }
  },

  getOrderStatusInfo(status) {
    switch (status) {
      case 'PENDING':
        return { label: 'Chờ thanh toán', color: '#475569', bg: '#f1f5f9' };
      case 'CONFIRMED':
        return { label: 'Đã xác nhận', color: '#2563eb', bg: '#eff6ff' };
      case 'PREPARING':
      case 'PROCESSING':
        return { label: 'Đang pha chế', color: '#7c3aed', bg: '#f5f3ff' };
      case 'READY':
        return { label: 'Sẵn sàng lấy', color: '#16a34a', bg: '#dcfce7' };
      case 'COMPLETED':
      case 'PAID':
        return { label: 'Hoàn thành', color: '#16a34a', bg: '#dcfce7' };
      case 'CANCELLED':
        return { label: 'Đã hủy', color: '#dc2626', bg: '#fee2e2' };
      default:
        return { label: status || 'Không rõ', color: '#64748b', bg: '#f1f5f9' };
    }
  },
};

export default formatService;
