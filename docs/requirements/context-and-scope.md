# 1. Bối cảnh và Vấn đề nghiệp vụ

## 1.1. Vấn đề nghiệp vụ
Hiện nay tại nhiều quán cà phê, vào các khung giờ cao điểm (sáng sớm hoặc giờ nghỉ trưa), việc gọi món tại quầy gặp nhiều bất cập:
- Khách hàng phải xếp hàng chờ đợi lâu để gọi món và thanh toán.
- Nhân viên dễ ghi nhầm order (độ ngọt, đá, topping) khi quán quá đông.
- Khách hàng không nắm được tiến độ đơn của mình, dẫn đến việc tụ tập chờ đợi gây ùn tắc tại quầy nhận đồ.

## 1.2. Giải pháp BrewLite
Hệ thống *BrewLite* được xây dựng nhằm cung cấp giải pháp đặt đồ uống trực tuyến qua ứng dụng Web:
- Khách hàng có thể quét mã QR tại bàn để xem menu, tự chọn topping, đặt món và thanh toán trực tiếp.
- Nhân viên nhận đơn tự động theo thời gian thực và cập nhật tiến độ pha chế.
- Khách hàng theo dõi trực tiếp trạng thái đơn hàng trên điện thoại mà không cần chen lấn.

---

# 2. Bảng phân tích Stakeholders

| Stakeholder | Mô tả vai trò | Mục tiêu & Kỳ vọng |
| :--- | :--- | :--- |
| *Khách hàng (Customer)* | Là người truy cập hệ thống để đặt món. | Giao diện thân thiện, xem menu nhanh, tùy chọn dễ dàng, thanh toán an toàn và biết chính xác khi nào món hoàn thành. |
| *Nhân viên quầy (Barista/Cashier)* | Tiếp nhận thông tin order và pha chế đồ uống. | Nhận đơn hàng chính xác, hiển thị rõ ràng từng tùy chọn của món, thao tác chuyển trạng thái đơn (Đang làm / Đã xong) thuận tiện. |
| *Quản lý cửa hàng (Store Manager)* | Giám sát hoạt động kinh doanh của quán. | Cập nhật được danh mục menu, chỉnh sửa giá cả, xem được thống kê số lượng đơn hàng và doanh thu theo ngày/ tháng. |
| *Đơn vị thanh toán (Payment Gateway)* | Bên thứ ba cung cấp dịch vụ thanh toán trực tuyến. | Đảm bảo tính bảo mật giao dịch, dữ liệu phản hồi (webhook/IPN) gửi về hệ thống chính xác và an toàn. |
| *Nhóm phát triển (Dev Team)* | Xây dựng, kiểm thử và vận hành hệ thống. | Thiết kế kiến trúc chuẩn (Next.js, Java Springboot, MySQL), quy trình CI/CD tự động, mã nguồn dễ bảo trì và mở rộng. |
| *Giảng viên nghiệm thu* | Đánh giá chất lượng đồ án môn học. | Hệ thống đáp ứng đúng yêu cầu nghiệp vụ, chạy mượt mà, tài liệu đặc tả đầy đủ và có kiểm thử rõ ràng. |

---

# 3. Phạm vi hệ thống (Scope)

| STT | Chức năng / Hạng mục | Trong phạm vi (In-Scope - MVP) | Ngoài phạm vi (Out-of-Scope) |
| :---: | :--- | :---: | :---: |
| 1 | *Duyệt Menu & Chi tiết món* | Có (Xem danh sách, phân loại, tìm kiếm) | - |
| 2 | *Tùy chọn món (Customization)* | Có (Chọn size, mức đường, mức đá, topping) | - |
| 3 | *Giỏ hàng* | Có (Thêm, xóa, sửa số lượng món) | - |
| 4 | *Đặt hàng & Thanh toán* | Có (Thanh toán qua cổng giả lập / Mock Sandbox) | Tích hợp nhiều cổng thanh toán thực tế phức tạp |
| 5 | *Theo dõi đơn hàng* | Có (Xem trạng thái: Chờ thanh toán -> Đã thanh toán -> Chuẩn bị món -> Chờ nhận món -> Hoàn thành đơn) | Thông báo qua tin nhắn SMS / Zalo |
| 6 | *Quản lý đơn hàng (Admin/Quầy)* | Có (Xem danh sách đơn đến, đổi trạng thái) | Báo cáo doanh thu nâng cao, dự báo số liệu |
| 7 | *Giao hàng tận nơi* | Không | Tích hợp bên thứ 3 (Ahamove, GrabExpress) |
| 8 | *Chương trình thành viên* | Không | Tích điểm đổi quà, voucher giảm giá phức tạp |
| 9 | *Quản lý kho nguyên liệu* | Không | Định lượng chi tiết hao hụt nguyên vật liệu |

---

# 4. Giả định, Ràng buộc và Rủi ro ban đầu

| Phân loại | Chi tiết | Ghi chú |
| :--- | :--- | :--- |
| *Giả định (Assumptions)* | - Người dùng có điện thoại thông minh kết nối Internet.<br>- Quầy pha chế có thiết bị (máy tính bảng/máy tính) kết nối mạng ổn định. | Đảm bảo hệ thống vận hành theo thời gian thực. |
| *Ràng buộc (Constraints)* | - Frontend: Bắt buộc dùng Next.js theo hướng Mobile-first/PWA.<br>- Backend: Bắt buộc dùng Java Springboot.<br>- Database: Bắt buộc dùng MySQL.<br>- Thời gian hoàn thành: 2 Sprints. | Tuân thủ đúng yêu cầu của đề tài. |
| *Rủi ro (Risks)* | - Mạng chập chờn gây mất kết nối WebSocket khi đồng bộ đơn.<br>- Thời gian cấu hình CI/CD và triển khai thực tế có thể phát sinh lỗi ngoài dự kiến. | Cần có cơ chế polling dự phòng khi mất kết nối. |