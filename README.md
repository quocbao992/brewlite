# Hướng Dẫn Triển Khai (Deployment Guide) - BrewLite

Tài liệu hướng dẫn các bước triển khai hệ thống BrewLite bao gồm Cơ sở dữ liệu (TiDB Cloud), Backend (Render Web Service) và Frontend (Vercel).

---

## 1. Cơ Sở Dữ Liệu: TiDB Cloud (MySQL Serverless)

1. Đăng nhập vào [TiDB Cloud](https://tidbcloud.com) và tạo một cụm miễn phí (Serverless Cluster). Chọn khu vực gần nhất (ví dụ: `ap-southeast-1` Singapore).
2. Tạo cơ sở dữ liệu (mặc định là `test` hoặc tạo database mới `app_db`).
3. Vào mục **Connect** trên bảng điều khiển, chọn loại kết nối là **Java (JDBC)** và lưu lại các thông số:
   - **Host**: `gateway01.ap-southeast-1.prod.aws.tidbcloud.com` (hoặc theo cụm được cấp)
   - **Port**: `4000`
   - **Username**: Có dạng `<prefix>.root`
   - **Password**: Mật khẩu được cấp khi tạo cluster
4. Định dạng chuỗi kết nối JDBC (bắt buộc cấu hình SSL):
   ```text
   jdbc:mysql://<HOST>:4000/<DATABASE>?sslMode=VERIFY_IDENTITY&serverTimezone=UTC&allowPublicKeyRetrieval=true
   ```

---

## 2. Backend: Render (Web Service)

1. Đăng nhập vào [Render Dashboard](https://dashboard.render.com) và chọn **New +** -> **Web Service**.
2. Kết nối với kho lưu trữ GitHub chứa mã nguồn dự án.
3. Thiết lập thông số Web Service:
   - **Name**: `brewlite-backend` (hoặc tên tùy chọn)
   - **Region**: Singapore (hoặc cùng khu vực với TiDB để tối ưu độ trễ)
   - **Branch**: `main`
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `Dockerfile` (sử dụng Dockerfile đa tầng ở thư mục gốc dự án)
   - **Instance Type**: Free
4. Cấu hình biến môi trường (**Environment Variables**):
   | Tên biến | Giá trị cấu hình |
   | :--- | :--- |
   | `PORT` | `8081` |
   | `SPRING_DATASOURCE_URL` | `jdbc:mysql://<HOST>:4000/<DATABASE>?sslMode=VERIFY_IDENTITY&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
   | `SPRING_DATASOURCE_USERNAME` | `<TIDB_USERNAME>` |
   | `SPRING_DATASOURCE_PASSWORD` | `<TIDB_PASSWORD>` |
   | `MOCK_GATEWAY_BASE_URL` | `https://<ten-backend-cua-ban>.onrender.com` |
5. Nhấn **Create Web Service** để tiến hành build và deploy.
6. Sau khi deploy thành công, Render sẽ cấp một URL công khai (ví dụ: `https://brewlite-backend-la8j.onrender.com`).
7. Lấy URL **Deploy Hook**:
   - Vào tab **Settings** của Web Service trên Render -> tìm mục **Deploy Hook**.
   - Sao chép URL Deploy Hook này để dùng cho quy trình tự động hóa CI/CD.

---

## 3. Frontend: Vercel

1. Cập nhật địa chỉ Backend Render vào tệp `apps/frontend/vercel.json` để cấu hình Reverse Proxy (tránh lỗi CORS và Mixed Content):
   ```json
   {
     "rewrites": [
       {
         "source": "/api/(.*)",
         "destination": "https://<ten-backend-cua-ban>.onrender.com/api/$1"
       },
       {
         "source": "/mock-gateway/(.*)",
         "destination": "https://<ten-backend-cua-ban>.onrender.com/mock-gateway/$1"
       },
       {
         "source": "/(.*)",
         "destination": "/index.html"
       }
     ]
   }
   ```
2. Đăng nhập vào [Vercel](https://vercel.com) và chọn **Add New...** -> **Project**.
3. Import kho lưu trữ GitHub của dự án.
4. Cấu hình thông số dự án trên Vercel:
   - **Root Directory**: Chọn `apps/frontend`
   - **Framework Preset**: `Vite`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
   - **Install Command**: `npm install`
5. Nhấn **Deploy**. Vercel sẽ build và cấp phát tên miền truy cập Frontend (ví dụ: `https://brewlite-olive.vercel.app`).

---

## 4. Tự Động Hóa CI/CD (GitHub Actions)

1. Vào kho lưu trữ trên GitHub -> **Settings** -> **Secrets and variables** -> **Actions**.
2. Thêm Secret mới:
   - **Name**: `PROD_DEPLOY_WEBHOOK`
   - **Secret**: Dán URL Deploy Hook đã lấy từ Render ở Bước 2.
3. Khi đẩy mã nguồn (push commit) lên nhánh `main`:
   - Vercel tự động kích hoạt tiến trình build và deploy lại Frontend.
   - GitHub Actions chạy quy trình kiểm thử, đóng gói và kích hoạt Deploy Hook để Render tự động cập nhật Backend mới nhất.
