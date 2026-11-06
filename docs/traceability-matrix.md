| Mã yêu cầu | Use-case/User Story | Thiết kế/Sơ đồ | Code/API | Test |
| :--- | :--- | :--- | :--- | :--- |
| **FR-01** | US-01 Xem danh sách thực đơn | `UI-Mockup-Menu`, `C4-CatalogModule` | `GET /api/v1/products` | `API-T01` |
| **FR-02** | US-02 Xem chi tiết & Tùy biến món | `UI-Mockup-ProductDetail` | `GET /api/v1/products/{id}` | `API-T02` |
| **FR-03** | UC-01 Quản lý giỏ hàng | `Activity-01 (Cart Flow)` | `POST /api/v1/cart` | `API-T03` |
| **FR-04** | UC-02 Đăng ký tài khoản | `Sequence-01 (Register)` | `POST /api/v1/auth/register` | `API-T04` |
| **FR-05** | UC-03 Đăng nhập & Xác thực | `Sequence-02 (Login JWT)` | `POST /api/v1/auth/login` | `API-T05` |
| **FR-06** | UC-04 Tạo đơn hàng (Checkout) | `Activity-02 (Checkout)`, `Sequence-03` | `POST /api/v1/orders` | `INT-T01` |
| **FR-07** | UC-05 Thanh toán không tiền mặt | `Sequence-04 (Payment)`, `C4-PaymentModule`| `POST /api/v1/payments` | `INT-T02` |
| **FR-08** | US-03 Xem mã QR & Trạng thái đơn | `UI-Mockup-OrderSuccess` | `GET /api/v1/orders/{id}` | `API-T06` |
| **FR-09** | US-04 Tra cứu lịch sử đơn hàng | `C4-OrderModule`, `ERD (Order)` | `GET /api/v1/orders/history` | `API-T07` |
| **FR-10** | UC-06 Cập nhật trạng thái pha chế | `UI-Mockup-Kanban`, `Sequence-05` | `PATCH /api/v1/orders/{id}/status`| `INT-T03` |
| **FR-11** | US-05 Tìm kiếm & Lọc đồ uống | `C4-CatalogModule` | `GET /api/v1/products/search` | `API-T08` |
| **FR-12** | UC-07 Quản lý danh mục & món (Admin)| `ERD (Product, Category)`, `C4-Admin` | `POST/PUT/DELETE /api/v1/admin/products`| `API-T09` |
| **FR-13** | US-06 Nhận thông báo hoàn tất đơn | `Sequence-06 (Notification/WebSocket)` | `GET /api/v1/notifications` | `INT-T04` |
| **FR-14** | UC-08 Áp dụng mã giảm giá | `Activity-03 (Apply Voucher)` | `POST /api/v1/cart/apply-voucher` | `API-T10` |