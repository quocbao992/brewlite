# ADR 005: Lựa chọn phong cách kiến trúc Monolith (Modular Monolith)

## Trạng thái
Đã chấp thuận (Accepted)

## Bối cảnh (Context)
Trong giai đoạn thiết kế kiến trúc hệ thống cho dự án BrewLite (Task 3.1), nhóm cần đưa ra quyết định lựa chọn phong cách kiến trúc tổng thể giữa Microservices và Monolithic Architecture. 
Hệ thống bao gồm các nghiệp vụ như quản lý thực đơn, đặt món, giỏ hàng, thanh toán và quản lý tồn kho. Dự án được triển khai bởi một nhóm phát triển nhỏ trong thời gian có hạn, với mục tiêu xây dựng một hệ thống hoạt động ổn định, dễ triển khai, dễ kiểm thử và đảm bảo tính toàn vẹn dữ liệu trong các giao dịch đặt hàng và thanh toán.

## Quyết định (Decision)
Nhóm quyết định áp dụng phong cách kiến trúc **Monolithic**, cụ thể là mô hình **Modular Monolith** kết hợp kiến trúc phân lớp (Layered Architecture):
* Toàn bộ các domain nghiệp vụ (Order, Payment, Catalog, Inventory, Customer, Cart,...) được đóng gói chung trong một codebase Spring Boot duy nhất và chạy trên cùng một tiến trình (single process).
* Các module được phân tách ranh giới rõ ràng thông qua cấu trúc package theo domain (Package-by-Feature), giao tiếp với nhau qua các interface/method call nội bộ thay vì thông qua mạng (network RPC/HTTP).

## Hệ quả (Consequences / Trade-offs)
* **Ưu điểm:**
  * **Đơn giản hóa việc phát triển và gỡ lỗi (Debugging):** Toàn bộ mã nguồn nằm chung một nơi, lập trình viên có thể dễ dàng trace log từ Controller, Service xuống Repository mà không cần cấu hình Distributed Tracing phức tạp.
  * **Giao dịch nhất quán (ACID Transactions):** Dễ dàng quản lý tính toàn vẹn dữ liệu thông qua cơ chế quản lý transaction cục bộ của Spring (`@Transactional`), đặc biệt là khi trừ tồn kho và tạo đơn hàng đồng thời, tránh được bài toán phân tán dữ liệu phức tạp (như mẫu Saga hay 2-Phase Commit).
  * **Tối ưu chi phí và hạ tầng:** Giảm thiểu độ trễ mạng (Network Latency) giữa các dịch vụ nội bộ; quy trình đóng gói thành một Docker image/file JAR duy nhất giúp việc thiết lập CI/CD và vận hành server đơn giản, tiết kiệm tài nguyên.
  * **Tốc độ ra mắt sản phẩm (Time-to-market):** Phù hợp với năng lực nhóm và thời hạn nộp đồ án, không tốn thời gian thiết kế các cơ chế Service Discovery, API Gateway phức tạp hay Message Queue rườm rà.
* **Nhược điểm (Trade-off):**
  * **Khó mở rộng độc lập (Horizontal Scaling):** Khi một module bị nghẽn tải (ví dụ tính năng quét thực đơn hoặc thanh toán), toàn bộ ứng dụng phải được nhân bản (scale) chung thay vì chỉ scale riêng module đó.
  * **Rủi ro phụ thuộc chéo (Tight Coupling):** Nếu các thành viên không tuân thủ nghiêm ngặt ranh giới giữa các module/service, mã nguồn có nguy cơ trở thành "Spaghetti code", gây khó khăn nếu sau này muốn tách thành Microservices độc lập.