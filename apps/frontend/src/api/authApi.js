import axiosClient from './axiosClient';

export const authApi = {
  login(data) {
    return axiosClient.post('/api/v1/auth/login', data);
  },
  register(data) {
    return axiosClient.post('/api/v1/auth/register', data);
  },
  logout(data = {}) {
    return axiosClient.post('/api/v1/auth/logout', data);
  },
  refreshToken(data) {
    return axiosClient.post('/api/v1/auth/refresh-token', data);
  },
  forgotPassword(data) {
    return axiosClient.post('/api/v1/auth/forgot-password', data);
  },
  resetPassword(data) {
    return axiosClient.post('/api/v1/auth/reset-password', data);
  },
};

export default authApi;
