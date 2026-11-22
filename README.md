# BrewLite Coffee — Hệ Thống Đặt Món & Quản Lý Quán Cà Phê

BrewLite là hệ thống đặt đồ uống tự động hóa được xây dựng theo kiến trúc hiện đại, tách biệt rõ ràng giữa giao diện người dùng (Frontend) và hệ thống xử lý nghiệp vụ (Backend). Nhằm đảm bảo hiệu suất vận hành ổn định, tính nhất quán của dữ liệu cũng như tối ưu hóa quy trình làm việc nhóm, dự án được phát triển dựa trên một hệ sinh thái công nghệ đồng bộ — từ nền tảng phát triển ứng dụng (Java Spring Boot, React/Vite), hệ quản trị cơ sở dữ liệu và bộ nhớ đệm (MySQL, Redis, Flyway), cho đến môi trường đóng gói và kiểm thử tự động (Docker, JUnit 5, Cypress, GitHub Actions).

Tài liệu README này cung cấp cái nhìn toàn diện về kiến trúc công nghệ, bộ công cụ phát triển, quản lý mã nguồn và quy trình triển khai hệ thống. Qua đó, người đọc và các thành viên mới có thể nhanh chóng nắm bắt môi trường kỹ thuật, thiết lập dự án cục bộ (local) và tham gia phối hợp phát triển một cách hiệu quả.


---

##  Mục Lục
- [Tính năng chính](#-tính-năng-chính)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc & Cấu trúc thư mục](#-kiến-trúc--cấu-trúc-thư-mục)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Hướng dẫn cài đặt & Chạy dự án ở Local](#-hướng-dẫn-cài-đặt--chạy-dự-án-ở-Local)
- [Kiểm thử (Testing)](#-kiểm-thử-testing)
- [Hướng dẫn đóng gói (Build)](#-hướng-dẫn-đóng-gói-build)
- [Hướng dẫn deploy](#-hướng-dẫn-deploy)

---

## Tính năng chính

###  Dành cho Khách hàng (Customer)
- Xác thực & Tài khoản: Đăng ký, đăng nhập (JWT + Refresh Token), quên/đặt lại mật khẩu.
- Thực đơn & Tùy chỉnh món: Xem danh sách đồ uống, chọn kích cỡ (`S`, `M`, `L`), thêm topping và ghi chú riêng cho từng ly.
- Giỏ hàng & Đặt hàng: Quản lý giỏ hàng đồng bộ, chọn hình thức dùng tại bàn (nhập số bàn) hoặc mang đi.
- Khuyến mãi & Điểm thưởng: Áp dụng mã voucher giảm giá và quy đổi điểm tích lũy (Loyalty Points).
- Thanh toán & Theo dõi đơn: Tích hợp cổng thanh toán (VNPay/VietQR, MoMo, Stripe) và theo dõi trạng thái đơn hàng.

###  Dành cho Nhân viên / Barista (Staff)
- Tiếp nhận hàng đợi đơn hàng (`PAID` $\rightarrow$ `PREPARING` $\rightarrow$ `READY` $\rightarrow$ `COMPLETED`).

###  Dành cho Quản trị viên (Admin)
- Quản lý thực đơn, topping, chương trình khuyến mãi và thống kê đơn hàng.
- Quản lý tồn kho nguyên liệu/sản phẩm và lịch sử xuất/nhập kho.

---

##  Công nghệ sử dụng

| Thành phần | Công nghệ |
| :--- | :--- |
| **Frontend** | React, Vite/CRA, Axios, CSS Modules / Custom CSS|
| **Backend** | Java 25, Spring Boot 4.0, Spring Security (JWT), Spring Data JPA, Flyway |
| **Cơ sở dữ liệu & Cache** | MySQL 8.0, Redis, Dbeaver |
| **Kiểm thử (Testing)** | JUnit 5 / Spring Boot Test (Integration Test), Cypress (E2E Test) |
| **Triển khai (DevOps)** | Docker, Docker Compose |

---

## Kiến trúc & Cấu trúc thư mục

```text
brewlite/
├── .github/
|    └──workflows/
├── apps/
│   ├── backend/                # Mã nguồn Spring Boot API (Port 8081)
│   │   ├── src/main/java/      # Controllers, Services, Repositories, Security, Domain
│   │   └── src/main/resources/ # application.properties, Flyway SQL migrations
│   └── frontend/               # Mã nguồn giao diện React (Port 3000)
├── test/                       # Bộ kiểm thử tự động Cypress E2E
│   ├── cypress.config.js     
│   └── e2e/                    # Kịch bản kiểm thử luồng đặt hàng & thanh toán
├── docker-compose.yml          # Cấu hình chạy toàn bộ hệ thống (Frontend, Backend, MySQL, Redis)
└── README.md
```
## Yêu cầu hệ thống (System Requirements)

### 1. Yêu cầu phần cứng & Môi trường
- **CPU:** Intel Core i5 hoặc tương đương trở lên.
- **RAM:** Tối thiểu 8 GB *(Khuyến nghị 16 GB nếu chạy đồng thời Docker + IDE)*.
- **Ổ cứng:** Trống tối thiểu 10 GB *(Khuyến nghị SSD trống ≥ 20 GB)*.
- **Hệ điều hành:** Windows 10/11 (64-bit), macOS hoặc Linux, có kết nối Internet.

### 2. Công cụ & Phần mềm cần cài đặt trước khi Clone

| Công cụ | Mục đích | Mức độ |
| :--- | :--- | :--- |
| **Git** | Clone mã nguồn và quản lý phiên bản | Bắt buộc |
| **Docker Desktop** | Chạy trọn gói các dịch vụ (MySQL, Redis, Backend, Frontend) qua Docker Compose | Bắt buộc *(nếu chạy Docker)* |
| **Java JDK (21+)** | Biên dịch và chạy ứng dụng Spring Boot *(dùng sẵn Maven Wrapper `./mvnw`)* | Bắt buộc *(nếu build/chạy Local)* |
| **Node.js (LTS 18+) & npm** | Cài đặt thư viện, chạy Frontend và bộ test E2E Cypress | Bắt buộc *(nếu chạy Local/Cypress)* |
| **MySQL 8.0 & Redis** | Hệ quản trị CSDL và bộ nhớ đệm | Bắt buộc *(nếu không dùng Docker)* |
| **VS Code / DBeaver / Postman** | Soạn thảo code, quản trị CSDL trực quan và kiểm thử API | Khuyến nghị |

### 3. Công nghệ sử dụng (Tech Stack)
- **Frontend:** React, Vite, Axios
- **Backend:** Java Spring Boot, Spring Security (JWT), Spring Data JPA, Flyway
- **Database & Cache:** MySQL 8.0, Redis
- **Testing & DevOps:** JUnit 5, Mockito, Cypress, Docker, Docker Compose, GitHub Actions

### 4. Lưu ý cấu hình trước khi chạy
1. **Khởi động Docker Desktop** trước khi chạy các lệnh `docker compose`.
2. **Biến môi trường:** Tạo file `.env` từ `.env.example` (nếu chạy ngoài cấu hình mặc định của Docker) và **không** commit thông tin mật khẩu/JWT secret lên GitHub.
3. **Kiểm tra sức khỏe dịch vụ:** Backend cần phản hồi bình thường tại endpoint kiểm tra trạng thái (ví dụ: `GET /health` hoặc `http://localhost:8081`).
## Hướng dẫn chạy local

Phần này hướng dẫn chạy hệ thống BrewLite (MySQL + Redis + Backend Spring Boot + Frontend React/Vite) trên máy cá nhân.

> Bản đã triển khai để xem nhanh: https://brewlite-olive.vercel.app/

### 1. Chuẩn bị

Cài đặt các công cụ sau (dùng chung cho cả hai cách chạy):

- **Git**
- **Docker Desktop** (phải đang chạy)
- **JDK 25** (khuyến nghị Eclipse Temurin 25)
- **Node.js 20 LTS** (kèm npm)

Kiểm tra phiên bản:

```bash
java -version    # phải hiện 25.x
node -v          # 20.x trở lên
```

Tải mã nguồn:

```bash
git clone <URL_REPOSITORY>
cd <TEN_THU_MUC_DU_AN>
```

Mọi lệnh `mvnw` bên dưới đều chạy ở **thư mục gốc** của dự án. Trên Windows PowerShell, dùng `.\mvnw` thay cho `./mvnw`.

> **Cách 1 và Cách 2 là hai lựa chọn thay thế nhau, chỉ cần chọn một**, không chạy nối tiếp. Nếu đã chạy Cách 1 rồi muốn chuyển sang Cách 2 (hoặc ngược lại), hãy làm theo ghi chú ở đầu Cách 2.

### 2. Cách 1 – Chạy toàn bộ bằng Docker Compose

Khởi động đủ 4 dịch vụ: MySQL, Redis, Backend, Frontend. Dockerfile của backend không tự biên dịch mà đóng gói file jar có sẵn, nên **phải build file jar trước**.

**Bước 1: Build file jar của backend** (ở thư mục gốc)

```bash
./mvnw clean package -pl apps/backend -am -DskipTests      # Windows PowerShell: .\mvnw clean package -pl apps/backend -am -DskipTests
```

Kết quả: file `apps/backend/target/brewlite-backend-1.0.0-SNAPSHOT.jar`. Lần đầu Maven tải thư viện nên hơi lâu.

**Bước 2: Khởi động hệ thống**

```bash
docker compose up --build
```

Lưu ý:

- Lần đầu khởi động, backend có thể báo `Communications link failure` và tự khởi động lại vài lần do MySQL chưa sẵn sàng. Đây là bình thường, đợi 1–2 phút.
- Hệ thống sẵn sàng khi log backend hiện `Started ... Application in ... seconds` và `DataSeeder hoan tat thanh cong`. Xem log bằng `docker compose logs -f backend`.
- **Mỗi khi sửa code backend**, phải chạy lại Bước 1 rồi `docker compose up --build` (hoặc `docker compose up -d --build backend`) để cập nhật file jar mới. Code frontend được gắn volume nên tự cập nhật khi lưu file.

**Bước 3: Mở ứng dụng trên trình duyệt**

Frontend đã được `docker compose` tự khởi động cùng các dịch vụ khác, không cần chạy thêm lệnh nào. Khi backend sẵn sàng, mở trình duyệt vào:

- **http://localhost:3000** (giao diện ứng dụng)

Đăng nhập bằng tài khoản ở mục 6 (ví dụ `customer01` / `Customer@123`). Nếu trang chưa mở được, xem log frontend bằng `docker compose logs -f frontend`.

Dừng hệ thống:

```bash
docker compose down        # giữ lại dữ liệu
docker compose down -v     # dừng và xóa luôn dữ liệu MySQL (về trạng thái ban đầu)
```

### 3. Cách 2 – Chạy Backend và Frontend thủ công

Dùng Docker cho MySQL và Redis, còn Backend và Frontend chạy trực tiếp trên máy (tiện để debug).

> Nếu đang chạy Cách 1, hãy `docker compose down` trước, vì hai cách dùng chung cổng 8081 và 3000. Lưu ý: lệnh `docker compose down` cũng tắt luôn MySQL và Redis, nên **bắt buộc phải làm Bước 1 bên dưới** để bật lại hai dịch vụ này trước khi chạy backend.

#### Bước 1: Khởi động MySQL và Redis

```bash
docker compose up -d db redis
```

Thông tin kết nối (đã khớp sẵn với `application.properties`, không cần sửa):

| Dịch vụ | Địa chỉ | Thông tin |
|---|---|---|
| MySQL | localhost:3306 | database `app_db`, user `root`, mật khẩu `rootpassword` |
| Redis | localhost:6379 | không có mật khẩu |

> Nếu máy đã cài MySQL/Redis riêng, hãy tắt chúng hoặc đổi cổng để tránh trùng cổng 3306 và 6379.

#### Bước 2: Chạy Backend

Chạy ở **thư mục gốc**:

```bash
./mvnw spring-boot:run -pl apps/backend        # Windows PowerShell: .\mvnw spring-boot:run -pl apps/backend
```

Backend chạy ở cổng `8081` (đổi bằng biến môi trường `PORT`). Thành công khi console hiện dòng `Started ... Application in ... seconds`. Các bảng dữ liệu được Hibernate tự tạo (`ddl-auto=update`) và dữ liệu mẫu được `DataSeeder` nạp tự động ở lần chạy đầu.

#### Bước 3: Chạy Frontend

Mở terminal mới, đứng ở **thư mục gốc dự án** (thư mục chứa `docker-compose.yml` và `pom.xml`) rồi chạy:

```bash
cd apps/frontend
npm install
npm run dev
```

Sau khi chạy xong, mở trình duyệt vào **http://localhost:3000**. Frontend chạy ở cổng `3000` và tự chuyển tiếp (proxy) các request `/api` và `/mock-gateway` sang `http://localhost:8081`, nên không cần cấu hình thêm.

### 4. Địa chỉ truy cập

| Dịch vụ | Địa chỉ |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8081 |
| MySQL | localhost:3306 |
| Redis | localhost:6379 |

### 5. Kiểm tra nhanh

Mở http://localhost:3000, đăng nhập bằng một tài khoản ở mục 6 (hoặc đăng ký tài khoản mới, mật khẩu tối thiểu 8 ký tự) và thấy danh sách thực đơn là hệ thống đã chạy đúng.

Thanh toán dùng **Mock Payment Gateway** mô phỏng MoMo/VNPay/Stripe, nên không cần khóa API thật. Cổng mô phỏng có độ trễ 500 ms và tỉ lệ thành công 60% (`mock.gateway.success-rate=0.6`), vì vậy thanh toán có thể thất bại ngẫu nhiên. Đây là hành vi có chủ đích để kiểm thử trạng thái `PAYMENT_FAILED`, bạn chỉ cần thanh toán lại.

### 6. Tài khoản và dữ liệu mẫu

Các tài khoản và dữ liệu dưới đây được `DataSeeder` tạo tự động khi backend khởi động (chỉ tạo nếu chưa tồn tại):

| Vai trò | Tài khoản | Mật khẩu |
|---|---|---|
| Quản trị (ADMIN) | `admin` | `Admin@123` |
| Barista | `barista01` | `Barista@123` |
| Thu ngân (CASHIER) | `cashier01` | `Cashier@123` |
| Khách hàng | `customer01`, `customer02`, `customer03` | `Customer@123` |

Dữ liệu mẫu đi kèm: 5 danh mục, 4 topping, 8 đồ uống/bánh và 5 đơn hàng ở các trạng thái `PAID`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`. Mỗi khách hàng mẫu có sẵn 120 điểm thưởng. Khách hàng mới cũng có thể tự đăng ký trên giao diện.

> Đây là cấu hình dùng cho phát triển và demo (mật khẩu database, tài khoản mẫu, secret của cổng thanh toán mô phỏng nằm trong mã nguồn), không dùng cho môi trường production.

### 7. Lỗi thường gặp

| Lỗi | Cách xử lý |
|---|---|
| `Connection refused: getsockopt` khi chạy backend thủ công (Cách 2) | MySQL (hoặc Redis) chưa chạy. Chạy `docker compose up -d db redis`, kiểm tra bằng `docker ps` thấy container `mysql_db` và `doancnpm_redis`, đợi khoảng 30 giây rồi chạy lại backend. |
| Docker build báo lỗi không tìm thấy `target/brewlite-backend-1.0.0-SNAPSHOT.jar` | Chưa build file jar. Chạy Bước 1 của Cách 1 rồi chạy lại `docker compose up --build`. |
| `Communications link failure` / `Access denied` khi chạy Docker | MySQL còn đang khởi động, đợi 1–2 phút (backend tự thử lại). Nếu vẫn lỗi, kiểm tra container `mysql_db` đang chạy bằng `docker ps`. Nếu từng đổi mật khẩu MySQL, xóa volume cũ: `docker compose down -v`. |
| `error during connect` / không kết nối được Docker | Docker Desktop chưa chạy. Mở Docker Desktop, đợi trạng thái *Engine running* rồi chạy lại. |
| `Port ... is already in use` (8081, 3000, 3306, 6379) | Có chương trình khác đang dùng cổng. Tắt nó, hoặc dừng Cách 1 (`docker compose down`) nếu đang chuyển sang Cách 2. Với MySQL/Redis cài sẵn trên máy, tắt dịch vụ đó. |
| `release version 25 not supported` / `invalid target release` | JDK trên máy không phải bản 25. Cài JDK 25 và kiểm tra `java -version` cùng biến `JAVA_HOME`. |
| `Permission denied` khi chạy `./mvnw` (Linux/macOS) | Chạy `chmod +x mvnw` rồi thử lại. |
| `./mvnw` không nhận diện trong PowerShell | Dùng `.\mvnw` thay cho `./mvnw`. |
| Frontend báo lỗi mạng / `ECONNREFUSED` khi gọi API | Backend chưa chạy xong hoặc chưa chạy. Kiểm tra log backend. |
| Thanh toán đôi lúc thất bại | Không phải lỗi: cổng thanh toán mô phỏng chỉ thành công 60%, hãy thanh toán lại. |
| Muốn xóa sạch dữ liệu để bắt đầu lại | `docker compose down -v`, sau đó chạy lại. `DataSeeder` sẽ nạp lại dữ liệu mẫu. |
##  Kiểm thử (Testing)

Dự án áp dụng mô hình kiểm thử 3 tầng (**Unit Test**, **Integration Test** và **End-to-End Test**) để đảm bảo tính chính xác từ từng hàm xử lý nghiệp vụ cho tới toàn bộ luồng trải nghiệm của người dùng trên hệ thống thật.

### 1. Kiểm thử đơn vị (Unit Tests — JUnit 5 & Mockito)

**Mục tiêu:** Kiểm tra độc lập từng hàm xử lý logic ở tầng Service/Utils (tính tổng tiền giỏ hàng, áp dụng mã khuyến mãi, quy đổi điểm thưởng, kiểm tra tồn kho, xác thực người dùng, sinh và giải mã JWT) mà không phụ thuộc vào cơ sở dữ liệu hay môi trường mạng.

- **Vị trí mã nguồn:** `apps/backend/src/test/java/vn/doancnpm`
- **Chạy toàn bộ Unit Test:**
  ```bash
  ./mvnw test -pl apps/backend
  ```
- **Chạy một file Unit Test cụ thể:**
  ```bash
  ./mvnw test -Dtest=CartServiceTest -pl apps/backend
  ```

---

### 2. Kiểm thử tích hợp (Integration Tests — Spring Boot Test & MockMvc)

**Mục tiêu:** Kiểm tra sự phối hợp giữa các tầng (`Controller` -> `Security Filter` -> `Service` -> `Repository` -> `Database`) thông qua các request HTTP API thực tế, xác minh mã trạng thái (`200`, `201`, `400`, `401`, `403`), cấu trúc dữ liệu JSON trả về và các ràng buộc dưới cơ sở dữ liệu.

- **Vị trí mã nguồn:** `apps/backend/src/test/java/vn/doancnpm/` (ví dụ: `PaymentControllerIntegrationTest.java`)
- **Chạy toàn bộ bộ test Backend (Unit + Integration):**
  ```bash
  ./mvnw test -pl apps/backend
  ```
- **Chạy riêng một file Integration Test:**
  ```bash
  cd apps/backend
  ./mvnw test -Dtest=PaymentControllerIntegrationTest -pl apps/backend
  ```
- **Xem báo cáo chi tiết sau khi chạy:** Mở các file kết quả tại thư mục `apps/backend/target/surefire-reports/`.

---

### 3. Kiểm thử đầu cuối (End-to-End Tests — Cypress)

**Mục tiêu:** Mô phỏng thao tác thực tế của khách hàng trên trình duyệt từ lúc truy cập trang chủ, đăng nhập (`customer01`), chọn món và tùy chỉnh kích cỡ/topping, thêm vào giỏ hàng, nhập thông tin nhận món, áp dụng mã ưu đãi cho tới khi hoàn tất thanh toán và kiểm tra đơn hàng.

- **Vị trí mã nguồn:**  `test/e2e/cypress/e2e/payment-flow.cy.js`
- **Điều kiện trước khi chạy:** Toàn bộ dịch vụ hệ thống phải đang hoạt động:
  - Frontend chạy tại `http://localhost:3000`
  - Backend API chạy tại `http://localhost:8081`
  - MySQL & Redis đã khởi động và có sẵn dữ liệu mẫu.

#### Bước 1: Cài đặt thư viện
```bash
cd test
npm install
```

#### Bước 2: Chạy kịch bản kiểm thử E2E
- **Cách 1 — Chạy trực quan trên giao diện Cypress (dùng khi demo):**
  ```bash
  npx cypress open
  ```
  1. Chọn **E2E Testing** -> Chọn trình duyệt (**Chrome** hoặc **Electron**).
  2. Chọn file `payment-flow.cy.js` để quan sát trình duyệt tự động thao tác và kiểm tra từng bước.

- **Cách 2 — Chạy tự động qua dòng lệnh (Headless mode):**
  ```bash
  npx cypress run
  ```
## Hướng dẫn đóng gói (Build)

Chạy ở thư mục gốc (nơi có `pom.xml`):

```bash
# Windows (PowerShell)
.\mvnw clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```

Kết quả: file `target/doan-cnpm-0.0.1-SNAPSHOT.jar`.

Lần đầu Maven sẽ tải thư viện nên hơi lâu.

### Chạy ứng dụng

**Cách 1: dùng Maven**
```bash
.\mvnw spring-boot:run
```

**Cách 2: chạy file jar**
```bash
java -jar target/doan-cnpm-0.0.1-SNAPSHOT.jar
```

**Cách 3: VS Code** - mở file `DoAnCnpmApplication.java` rồi bấm **Run** phía trên dòng `main`.

Chạy thành công khi console hiện:
```
Tomcat started on port 8081
Started DoAnCnpmApplication in ... seconds
```

API chạy tại: `http://localhost:8081`

Dừng ứng dụng: nhấn **Ctrl+C** trong terminal. Phải dừng app cũ trước khi chạy lại, nếu không sẽ báo cổng 8081 đang bị chiếm.



## Hướng Dẫn Triển Khai (Deployment Guide) - BrewLite

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
