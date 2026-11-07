import axiosClient from './axiosClient';

export const productApi = {
  search(params = {}) {
    return axiosClient.get('/api/v1/products', { params });
  },
  getById(id) {
    return axiosClient.get(`/api/v1/products/${id}`);
  },
  getByCategory(categoryId, isAvailable = true) {
    return axiosClient.get(`/api/v1/products/category/${categoryId}`, {
      params: { isAvailable },
    });
  },
  getFeatured() {
    return axiosClient.get('/api/v1/products/featured');
  },
  create(data) {
    return axiosClient.post('/api/v1/products', data);
  },
  update(id, data) {
    return axiosClient.put(`/api/v1/products/${id}`, data);
  },
  delete(id) {
    return axiosClient.delete(`/api/v1/products/${id}`);
  },
};

export default productApi;
