import axiosClient from './axiosClient';

export const cartApi = {
  getCart(customerId) {
    return axiosClient.get('/api/v1/cart', {
      params: { customerId },
    });
  },
  addItem(data) {
    return axiosClient.post('/api/v1/cart/items', data);
  },
  updateItemQuantity(cartItemId, quantity) {
    return axiosClient.put(`/api/v1/cart/items/${cartItemId}`, { quantity });
  },
  removeItem(cartItemId) {
    return axiosClient.delete(`/api/v1/cart/items/${cartItemId}`);
  },
  clearCart(customerId) {
    return axiosClient.delete('/api/v1/cart', {
      params: { customerId },
    });
  },
};

export default cartApi;
