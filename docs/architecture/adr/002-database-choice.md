# ADR 002: Lựa chọn MySQL làm Cơ sở dữ liệu chính

## Trạng thái
Đã chấp thuận (Accepted)

## Bối cảnh (Context)
Đề bài đưa ra gợi ý sử dụng PostgreSQL kèm cơ chế quản lý migration. Hệ thống đòi hỏi một cơ sở dữ liệu quan hệ (RDBMS) có persistent storage ổn định để lưu trữ thông tin sản phẩm, đơn hàng, và lịch sử giao dịch.

## Quyết định (Decision)
Nhóm quyết định sử dụng **MySQL** làm cơ sở dữ liệu chính, kết hợp cùng công cụ **Flyway** (hoặc Liquibase) để đáp ứng yêu cầu cơ chế tự động migration schema (DDL scripts).

## Hệ quả (Consequences / Trade-offs)
* **Ưu điểm:** 
  * MySQL cực kỳ phổ biến, dễ dàng cấu hình và có sẵn nhiều tài liệu tích hợp với Spring Boot.
  * Tốc độ đọc (read) của MySQL rất tốt cho các query đơn giản (như lấy danh sách món ăn, menu).
* **Nhược điểm (Trade-off):** 
  * Thiếu một số tính năng xử lý kiểu dữ liệu JSON mạnh mẽ và các phép toán phức tạp vốn là thế mạnh của PostgreSQL. Tuy nhiên, hệ thống quản lý đơn hàng hiện tại chưa yêu cầu các tính năng nâng cao này.