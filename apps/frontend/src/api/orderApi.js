import axiosClient from './axiosClient';

export const orderApi = {
  checkout(data) {
    return axiosClient.post('/api/v1/orders/checkout', data);
  },
  getHistory(params = {}) {
    return axiosClient.get('/api/v1/orders', { params });
  },
  getQueue(statusList = []) {
    return axiosClient.get('/api/v1/orders/queue', {
      params: { status: statusList.join(',') },
    });
  },
  getByCode(orderCode) {
    return axiosClient.get(`/api/v1/orders/code/${orderCode}`);
  },
  getById(id) {
    return axiosClient.get(`/api/v1/orders/${id}`);
  },
  cancel(id, reason = 'Khách hàng yêu cầu hủy qua ứng dụng') {
    return axiosClient.put(`/api/v1/orders/${id}/cancel`, { reason });
  },
  updateStatus(id, status) {
    return axiosClient.put(`/api/v1/orders/${id}/status`, { status });
  },
  confirmOrder(id) {
    return axiosClient.post(`/api/v1/orders/${id}/confirm`);
  },
  adminGetAll(params = {}) {
    return axiosClient.get('/api/admin/orders', { params });
  },
};

export default orderApi;
