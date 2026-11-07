import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const status = error.response ? error.response.status : null;
    let rawMessage =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'Đã xảy ra lỗi kết nối tới máy chủ';

    // Chuyển đổi các thông báo lỗi tiếng Anh mặc định sang tiếng Việt
    let message = rawMessage;
    if (typeof rawMessage === 'string') {
      const lower = rawMessage.toLowerCase();
      if (lower.includes('method not allowed') || status === 405) {
        message = 'Thao tác không được hỗ trợ hoặc phương thức gửi không đúng.';
      } else if (lower.includes('bad request') || status === 400) {
        message = rawMessage.includes('Bad Request')
          ? 'Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin.'
          : rawMessage;
      } else if (lower.includes('unauthorized') || status === 401) {
        message = 'Phiên đăng nhập đã hết hạn hoặc tài khoản chưa được xác thực.';
      } else if (lower.includes('forbidden') || status === 403) {
        message = 'Bạn không có quyền truy cập vào chức năng này.';
      } else if (lower.includes('not found') || status === 404) {
        message = 'Không tìm thấy dữ liệu yêu cầu trên hệ thống.';
      } else if (lower.includes('network error')) {
        message = 'Không thể kết nối đến máy chủ Backend. Vui lòng kiểm tra dịch vụ.';
      } else if (status >= 500) {
        message = 'Máy chủ gặp sự cố khi xử lý dữ liệu. Vui lòng thử lại sau.';
      }
    }

    if (status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }

    return Promise.reject({
      status,
      message,
      data: error.response?.data,
      originalError: error,
    });
  }
);

export default axiosClient;
