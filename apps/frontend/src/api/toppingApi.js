import axiosClient from './axiosClient';

export const toppingApi = {
  getAll(status) {
    const params = status !== undefined ? { status } : {};
    return axiosClient.get('/api/v1/toppings', { params });
  },

  getById(id) {
    return axiosClient.get(`/api/v1/toppings/${id}`);
  },

  create(data) {
    return axiosClient.post('/api/v1/toppings', data);
  },

  update(id, data) {
    return axiosClient.put(`/api/v1/toppings/${id}`, data);
  },

  updateStatus(id, status) {
    return axiosClient.patch(`/api/v1/toppings/${id}/status`, null, {
      params: { status },
    });
  },
};

export default toppingApi;
