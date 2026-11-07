# TASK 1.2: YÊU CẦU CHỨC NĂNG VÀ PHI CHỨC NĂNG (BREWLITE)


## 1. Yêu cầu chức năng (Functional Requirements - FR)


| Mã FR | Tên chức năng | Phân nhóm | Mô tả chi tiết | Ưu tiên (MoSCoW) | Tiêu chí chấp nhận (Acceptance Criteria) |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **FR-01** | Hiển thị thực đơn (Menu) | Menu & Tìm kiếm | Hiển thị danh mục đồ uống kèm hình ảnh, tên món, giá niêm yết cơ bản và trạng thái còn/hết hàng. | **Must** | - Người dùng có thể xem danh sách sản phẩm theo từng danh mục (Trà, Cà phê,...).<br>- Sản phẩm hết hàng phải hiển thị nhãn "Hết hàng" và vô hiệu hóa nút chọn. |
| **FR-02** | Xem chi tiết & Tùy biến món | Chi tiết món | Cho phép chọn size (S/M/L), độ ngọt/đá và các loại topping đi kèm. Giá được cập nhật trực tiếp theo lựa chọn. | **Must** | - Thay đổi size/topping phải tính lại đơn giá ngay trên giao diện.<br>- Bắt buộc chọn ít nhất 1 size trước khi thêm vào giỏ. |
| **FR-03** | Quản lý giỏ hàng | Giỏ hàng | Thêm mới, cập nhật số lượng, xóa từng món hoặc làm trống giỏ hàng; tự động tính tổng tiền tạm tính. | **Must** | - Giỏ hàng hiển thị đúng danh sách món, option đã chọn, đơn giá và tổng thành tiền.<br>- Nếu cập nhật số lượng về 0, hệ thống hỏi xác nhận xóa. |
| **FR-04** | Đăng ký tài khoản | Xác thực (Auth) | Khách hàng đăng ký tài khoản mới bằng Email/Số điện thoại và mật khẩu. | **Must** | - Kiểm tra định dạng email/SĐT hợp lệ.<br>- Mật khẩu tối thiểu 8 ký tự, có xác nhận mật khẩu.<br>- Báo lỗi nếu tài khoản đã tồn tại. |
| **FR-05** | Đăng nhập & Xác thực JWT | Xác thực (Auth) | Khách hàng đăng nhập hệ thống; trả về Access Token (JWT) để ủy quyền các API tiếp theo. | **Must** | - Đăng nhập đúng trả về JWT token và lưu phiên làm việc.<br>- Đăng nhập sai quá số lần quy định thì hiển thị cảnh báo.<br>- Hỗ trợ đăng xuất và hủy token phiên làm việc. |
| **FR-06** | Tạo đơn hàng (Checkout) | Đặt hàng | Khách hàng xác nhận giỏ hàng, nhập thông tin liên hệ/ghi chú để tiến hành tạo đơn hàng mới. | **Must** | - Đơn hàng được khởi tạo với trạng thái `PENDING`.<br>- Tự động trừ hoặc kiểm tra lượng tồn kho trước khi chuyển sang thanh toán. |
| **FR-07** | Thanh toán không tiền mặt | Thanh toán | Tích hợp cổng thanh toán mô phỏng/Ví điện tử/Thẻ để thanh toán đơn hàng trực tuyến. | **Must** | - Giỏ hàng có tối thiểu 1 món mới cho phép ấn thanh toán.<br>- Thanh toán thành công: Đơn hàng chuyển sang trạng thái `PAID`.<br>- Thanh toán thất bại: Đơn chuyển sang `PAYMENT_FAILED`, giữ nguyên giỏ hàng. |
| **FR-08** | Xác nhận & Tra cứu đơn | Xác nhận đơn | Hiển thị mã đơn hàng (Order ID), mã QR nhận đồ và trạng thái đơn ngay sau khi thanh toán thành công. | **Must** | - Hiển thị mã đơn hàng định dạng duy nhất.<br>- Cung cấp mã QR code để Barista có thể quét nhận diện đơn tại quầy. |
| **FR-09** | Lịch sử đơn hàng | Lịch sử | Khách hàng tra cứu danh sách các đơn hàng đã thực hiện kèm trạng thái chi tiết. | **Must** | - Hiển thị danh sách đơn phân trang theo ngày đặt.<br>- Xem lại chi tiết từng đơn hàng (món, giá, phương thức thanh toán, trạng thái). |
| **FR-10** | Cập nhật trạng thái pha chế | Barista/Admin | Nhân viên quầy/Barista tiếp nhận đơn, cập nhật trạng thái (`PENDING` $\rightarrow$ `PAID` $\rightarrow$ `PREPARING` $\rightarrow$ `COMPLETED` $\rightarrow$ `CANCELLED`). | **Should** | - Barista có màn hình Kanban/danh sách đơn mới theo thời gian thực.<br>- Cập nhật trạng thái thành công và phản hồi về DB ngay lập tức. |
| **FR-11** | Tìm kiếm & Lọc đồ uống | Menu & Tìm kiếm | Hỗ trợ tìm kiếm theo từ khóa tên đồ uống và lọc theo tầm giá hoặc phân loại. | **Should** | - Trả kết quả tìm kiếm theo thời gian thực trong vòng < 300ms.<br>- Hiển thị thông báo khi không tìm thấy kết quả. |
| **FR-12** | Quản lý danh mục & món | Admin | Chủ quán/Quản trị viên thêm, sửa, xóa món ăn, chỉnh sửa giá và bật/tắt trạng thái hết hàng. | **Should** | - Chỉ tài khoản có role `ADMIN` hoặc `STORE_MANAGER` mới truy cập được.<br>- Thay đổi giá và trạng thái phản ánh tức thời ra menu khách hàng. |
| **FR-13** | Thông báo trạng thái đơn hàng | Thông báo | Gửi thông báo đến thiết bị/trình duyệt khi đồ uống đã pha chế xong để khách đến lấy. | **Could** | - Nhận thông báo popup/toast khi đơn chuyển sang `COMPLETED`. |
| **FR-14** | Áp dụng giảm giá (Loyalty points) | Khuyến mãi | Khách hàng nhập mã ưu đãi để chiết khấu trực tiếp vào tổng tiền thanh toán. | **Could** | - Kiểm tra hạn sử dụng và điều kiện áp dụng của mã voucher.<br>- Tính đúng số tiền giảm tối đa theo thiết lập. |
| **FR-15** | Đánh giá & Phản hồi đồ uống | Tương tác | Khách hàng đánh giá sao (1-5 sao) và để lại nhận xét sau khi nhận đơn thành công. | **Won't** | - Tạm thời chưa triển khai trong phạm vi MVP Sprint 1 & 2. |

---

## 2. Yêu cầu phi chức năng (Non-Functional Requirements - NFR)

| Mã NFR | Tiêu chí | Mô tả chi tiết | Đo lường / Tiêu chí nghiệm thu |
| :--- | :--- | :--- | :--- |
| **NFR-01** | **Hiệu năng (Performance)** | Tốc độ phản hồi của hệ thống và API backend đối với bộ dữ liệu mẫu. | - Thời gian phản hồi API (Response Time) $\le 500\text{ ms}$ cho 95% request ($p95$).<br>- Thời gian tải trang ban đầu (First Contentful Paint) $< 1.5\text{ s}$. |
| **NFR-02** | **Bảo mật (Security)** | Đảm bảo tính bí mật, toàn vẹn và xác thực của tài khoản cũng như giao dịch. | - Mật khẩu người dùng được băm an toàn bằng thuật toán mạnh (ví dụ: `bcrypt` với cost factor $\ge 10$).<br>- Phiên xác thực sử dụng JSON Web Token (JWT) có thời hạn sống (exp), ký bằng secret an toàn.<br>- Dữ liệu đầu vào (Input) từ client bắt buộc validate nghiêm ngặt ở cả FE và BE (chống SQL Injection, XSS). |
| **NFR-03** | **Tính khả dụng (Usability & Compatibility)** | Giao diện thân thiện, dễ sử dụng cho khách hàng tại quầy và nhân viên. | - Tương thích hoàn toàn trên các trình duyệt hiện đại (Chrome, Safari, Firefox, Edge).<br>- Giao diện chuẩn Responsive Web Design, tối ưu hóa cho màn hình di động (Mobile-first, viewport 360px - 430px).<br>- Luồng đặt đồ uống từ chọn món đến hoàn tất thanh toán không vượt quá 5 bước chạm. |
| **NFR-04** | **Tính sẵn sàng & Tin cậy (Reliability & Idempotency)** | Đảm bảo không xảy ra sai lệch dữ liệu tài chính trong quá trình thanh toán. | - Hỗ trợ cơ chế Idempotency Key cho API thanh toán nhằm ngăn chặn việc trừ tiền trùng lặp khi người dùng bấm nhiều lần.<br>- Hệ thống phục hồi giao dịch tự động khi có lỗi mạng bất thường. |
| **NFR-05** | **Khả năng triển khai (Deployability & DevOps)** | Khả năng đóng gói và triển khai nhanh chóng theo quy trình CI/CD. | - Toàn bộ mã nguồn backend và frontend được container hóa bằng Docker.<br>- Hỗ trợ pipeline CI tự động build, test và deploy lên môi trường staging/production. |

---

## 3. Sơ đồ Use Case tổng quát (Overall Use Case Diagram)

<p align="center">
  <img src="../diagrams/overall-use-case.png" style="width: 100%; max-width: none; height: auto;" alt="Sơ đồ Use-Case tổng quát">
</p>