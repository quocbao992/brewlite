import axiosClient from './axiosClient';

export const inventoryApi = {
  // Lấy tổng quan toàn bộ tồn kho (Sản phẩm & Topping) gồm warehouseStock và stock đang bán
  getOverview() {
    return axiosClient.get('/api/v1/inventory/overview');
  },

  // Nhập thêm số lượng tồn kho (kho lưu trữ)
  addWarehouseStock({ type, id, amount, reason }) {
    return axiosClient.post('/api/v1/inventory/warehouse/add', {
      type,
      id,
      amount,
      reason,
    });
  },

  // Chuyển từ tồn kho sang đang bán (trừ warehouseStock, cộng stock đang bán)
  transferToSelling({ type, id, amount }) {
    return axiosClient.post('/api/v1/inventory/transfer-to-selling', {
      type,
      id,
      amount,
    });
  },

  // Lấy toàn bộ nhật ký biến động kho
  getLogs() {
    return axiosClient.get('/api/v1/inventory/logs');
  },

  // Lấy log riêng của từng sản phẩm
  getProductLogs(productId) {
    return axiosClient.get(`/api/v1/inventory/logs/products/${productId}`);
  },

  // Lấy log riêng của từng topping
  getToppingLogs(toppingId) {
    return axiosClient.get(`/api/v1/inventory/logs/toppings/${toppingId}`);
  },
};

export default inventoryApi;

