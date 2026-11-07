# ADR 001: Lựa chọn Spring Boot làm Backend Framework

## Trạng thái
Đã chấp thuận (Accepted)

## Bối cảnh (Context)
Yêu cầu ban đầu của đề bài đề xuất sử dụng NestJS theo kiến trúc module/domain cho backend. Tuy nhiên, trong quá trình đánh giá năng lực công nghệ và định hướng của dự án, nhóm nhận thấy cần một framework có tính định kiểu tĩnh chặt chẽ , quản lý dependency injection mạnh mẽ và thư viện ORM ổn định để xử lý logic giao dịch thanh toán phức tạp.

## Quyết định (Decision)
Nhóm quyết định thay đổi sang sử dụng **Spring Boot (Java)** kết hợp kiến trúc phân lớp (Layered Architecture) phân chia theo các Domain Module nội bộ.

## Hệ quả (Consequences / Trade-offs)
* **Ưu điểm:** 
  * Tận dụng tối đa kinh nghiệm lập trình Java của các thành viên trong nhóm.
  * Hệ sinh thái Spring Data JPA hỗ trợ thao tác cơ sở dữ liệu mạnh mẽ và bảo mật cao.
  * Cấu trúc thư mục rõ ràng, dễ bảo trì theo tiêu chuẩn doanh nghiệp.
* **Nhược điểm (Trade-off):** 
  * Ứng dụng Java Spring Boot tiêu tốn nhiều bộ nhớ (RAM) và có thời gian khởi động (startup time) chậm hơn so với NestJS (Node.js).