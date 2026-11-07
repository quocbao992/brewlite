FROM node:20-alpine

WORKDIR /app

# Cài đặt dependencies trước để tối ưu hóa Docker layer caching
COPY package*.json ./
RUN npm install

# Sao chép toàn bộ mã nguồn frontend
COPY . .

EXPOSE 3000

# Chạy Vite dev server lắng nghe trên tất cả network interfaces
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

