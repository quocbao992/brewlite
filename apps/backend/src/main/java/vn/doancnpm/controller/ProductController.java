package vn.doancnpm.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.dto.Request.ProductRequest;
import vn.doancnpm.dto.Response.PageResponse;
import vn.doancnpm.dto.Response.ProductResponse;
import vn.doancnpm.service.ProductService;

@RestController
@RequestMapping(value = "/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Danh sách + lọc + tìm kiếm + phân trang
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.search(categoryId, keyword, isAvailable, page, size));
    }

    // Toàn bộ sản phẩm của một danh mục: GET /api/v1/products/category/1?isAvailable=true
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Boolean isAvailable) {
        return ResponseEntity.ok(productService.getByCategory(categoryId, isAvailable));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeatured(@RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(productService.getFeatured(limit));
    }

    @GetMapping("/menu")
    public ResponseEntity<List<ProductResponse>> getMenu() {
        return ResponseEntity.ok(productService.getMenu());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ProductResponse> updatePrice(@PathVariable Long id,
                                                       @RequestParam BigDecimal price) {
        return ResponseEntity.ok(productService.updatePrice(id, price));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateStatus(@PathVariable Long id,
                                                        @RequestParam boolean status) {
        return ResponseEntity.ok(productService.updateStatus(id, status));
    }

    // Xóa (xóa mềm)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa sản phẩm thành công"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return buildError(HttpStatus.BAD_REQUEST,
            "Tham số '" + ex.getName() + "' có giá trị không hợp lệ: " + ex.getValue());
}

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Thiếu tham số: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Body JSON không hợp lệ");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<Map<String, Object>> handleConflict(ObjectOptimisticLockingFailureException ex){
            return buildError(HttpStatus.CONFLICT, "Sản phẩm vừa được người khác cập nhật, vui lòng tải lại và thử lại");
        }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        ex.printStackTrace(); // In toàn bộ stack trace đỏ vào Terminal của IDE
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
    
}