import axiosClient from './axiosClient';

export const customerApi = {
  getProfile(userId) {
    return axiosClient.get('/api/v1/customers/profile', {
      params: { userId },
    });
  },
  updateProfile(userId, data) {
    return axiosClient.put('/api/v1/customers/profile', data, {
      params: { userId },
    });
  },
  getLoyaltyPoints(userId) {
    return axiosClient.get('/api/v1/customers/loyalty-history', {
      params: { userId },
    });
  },
  changePassword(userId, data) {
    return axiosClient.put('/api/v1/users/change-password', data, {
      params: { userId },
    });
  },
};

export default customerApi;
