import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  // Nếu chạy trong Docker container thì trỏ về host.docker.internal, nếu chạy ngoài máy thật thì trỏ về localhost
  const backendTarget = env.VITE_BACKEND_URL || process.env.VITE_BACKEND_URL || 'http://localhost:8081';

  return {
    plugins: [react()],
    server: {
      host: '0.0.0.0', // Cho phép truy cập từ trình duyệt máy host vào container port 3000
      port: 3000,
      watch: {
        usePolling: true, // Đảm bảo Hot Reload mượt mà khi chạy qua Docker Volume trên Windows
      },
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
          secure: false,
        },
        '/mock-gateway': {
          target: backendTarget,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  };
});
