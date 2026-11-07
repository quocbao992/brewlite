import axiosClient from './axiosClient';

export const paymentApi = {
  // Gửi yêu cầu thanh toán (nhận QR / URL từ backend)
  requestPayment(data) {
    return axiosClient.post('/api/v1/payments/request', data);
  },

  // Cổng thanh toán gọi webhook / hoặc UI mô phỏng xác nhận thanh toán
  notifyPayment(secret = 'mock-secret', data) {
    return axiosClient.post('/api/v1/payments/notify', data, {
      headers: {
        'X-Gateway-Secret': secret,
      },
    });
  },

  // Xem chi tiết giao dịch thanh toán
  getById(paymentId) {
    return axiosClient.get(`/api/v1/payments/${paymentId}`);
  },

  // Xem các giao dịch thanh toán theo đơn hàng
  getByOrder(orderId) {
    return axiosClient.get(`/api/v1/payments/order/${orderId}`);
  },
};

export default paymentApi;
