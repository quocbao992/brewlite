# ADR 003: Lựa chọn React và Vite cho Frontend

## Trạng thái
Đã chấp thuận (Accepted) - Cập nhật thay thế cho quyết định dùng Next.js trước đó.

## Bối cảnh (Context)
Hệ thống đặt đồ uống BrewLite phục vụ hai nhóm đối tượng chính: khách hàng đặt món trực tiếp tại quầy thông qua quét mã QR trên thiết bị di động, và nhân viên/quản lý vận hành hệ thống đơn hàng trên máy tính/máy tính bảng. Ban đầu, dự án dự kiến dùng Next.js. Tuy nhiên, để tối ưu hóa thời gian phát triển, giảm bớt sự phức tạp của Server-Side Rendering (SSR) không thực sự cần thiết cho một ứng dụng mang tính nội bộ/đặt hàng qua QR (không đặt nặng vấn đề SEO), nhóm cần một giải pháp frontend nhẹ nhàng, tốc độ build nhanh và dễ dàng triển khai (deploy).

## Quyết định (Decision)
Nhóm quyết định chuyển sang sử dụng **React** kết hợp công cụ build **Vite** làm nền tảng phát triển toàn bộ giao diện người dùng theo mô hình **Single Page Application (SPA)**. 

## Hệ quả (Consequences / Trade-offs)
- **Ưu điểm:**
  * **Tốc độ phát triển & Build cực nhanh:** Vite sử dụng esbuild giúp thời gian khởi động môi trường dev (dev server) gần như tức thì và tính năng Hot Module Replacement (HMR) cực kỳ mượt mà, giúp tăng năng suất của developer.
  * **Trải nghiệm SPA mượt mà:** Khách hàng và nhân viên khi thao tác (chuyển đổi giữa các tab menu, thêm vào giỏ hàng, xem dashboard) sẽ không bị tải lại trang, mang lại cảm giác mượt mà như một ứng dụng độc lập.
  * **Triển khai (Deploy) đơn giản & Chi phí thấp:** Ứng dụng sau khi build chỉ là các file tĩnh (HTML, CSS, JS), có thể dễ dàng host trên bất kỳ dịch vụ lưu trữ tĩnh nào (Vercel, Netlify, AWS S3, GitHub Pages) mà không cần duy trì Node.js server như Next.js.
  * **Tái sử dụng linh hoạt:** Dễ dàng xây dựng giao diện Responsive chuẩn Mobile-first cho khách hàng và bố cục bảng điều khiển (Dashboard) cho nhân viên Barista/Admin trên cùng một codebase React.

- **Nhược điểm (Trade-off):**
  * **SEO hạn chế:** Vì là mô hình Client-Side Rendering (CSR), bot của các công cụ tìm kiếm sẽ khó đọc nội dung trang web hơn so với Next.js (SSR). *Tuy nhiên, đây là sự đánh đổi chấp nhận được vì BrewLite là hệ thống đặt hàng qua QR và quản lý nội bộ, không cần SEO mạnh.*
  * **Thời gian tải trang đầu tiên (Initial Load Time):** Có thể chậm hơn một chút so với SSR nếu kích thước bundle JavaScript quá lớn. Nhóm sẽ cần áp dụng các kỹ thuật như Code Splitting (Lazy load component/route) để khắc phục nhược điểm này.