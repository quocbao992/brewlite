import axiosClient from './axiosClient';

export const categoryApi = {
  getAll(params = {}) {
    return axiosClient.get('/api/v1/categories', { params });
  },
  getById(id) {
    return axiosClient.get(`/api/v1/categories/${id}`);
  },
  create(data) {
    return axiosClient.post('/api/v1/categories', data);
  },
  update(id, data) {
    return axiosClient.put(`/api/v1/categories/${id}`, data);
  },
  delete(id) {
    return axiosClient.delete(`/api/v1/categories/${id}`);
  },
};

export default categoryApi;
