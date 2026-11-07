import axiosClient from './axiosClient';

export const promotionApi = {
  // Lấy toàn bộ danh sách mã khuyến mãi
  getAll() {
    return axiosClient.get('/api/promotions');
  },

  // Lấy thông tin 1 mã khuyến mãi theo ID
  getById(id) {
    return axiosClient.get(`/api/promotions/${id}`);
  },

  // Tạo mới mã khuyến mãi
  create(data) {
    return axiosClient.post('/api/promotions', data);
  },

  // Cập nhật mã khuyến mãi
  update(id, data) {
    return axiosClient.put(`/api/promotions/${id}`, data);
  },

  // Xóa mã khuyến mãi
  delete(id) {
    return axiosClient.delete(`/api/promotions/${id}`);
  },

  // Bật / tắt trạng thái hoạt động
  toggleStatus(id) {
    return axiosClient.patch(`/api/promotions/${id}/status`);
  },

  // Kiểm tra & tính toán mức giảm giá khi áp dụng mã voucher
  validate(code, amount) {
    return axiosClient.get('/api/promotions/validate', {
      params: { code, amount },
    });
  },
};

export default promotionApi;

