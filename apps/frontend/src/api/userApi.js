import axiosClient from './axiosClient';

export const userApi = {
  getProfile(userId) {
    return axiosClient.get('/api/v1/users/me', {
      params: { userId },
    });
  },
  updateProfile(userId, data) {
    return axiosClient.put('/api/v1/users/me', data, {
      params: { userId },
    });
  },
  changePassword(userId, data) {
    return axiosClient.put('/api/v1/users/change-password', data, {
      params: { userId },
    });
  },
  adminGetAllUsers(params = {}) {
    return axiosClient.get('/api/v1/admin/users', { params });
  },
  adminGetUserById(id) {
    return axiosClient.get(`/api/v1/admin/users/${id}`);
  },
  adminCreateUser(data) {
    return axiosClient.post('/api/v1/admin/users', data);
  },
  adminUpdateUser(id, data) {
    return axiosClient.put(`/api/v1/admin/users/${id}`, data);
  },
  adminUpdateStatus(id, status) {
    return axiosClient.patch(`/api/v1/admin/users/${id}/status`, null, {
      params: { status },
    });
  },
  adminUpdateRole(id, role) {
    return axiosClient.patch(`/api/v1/admin/users/${id}/role`, null, {
      params: { role },
    });
  },
};

export default userApi;
