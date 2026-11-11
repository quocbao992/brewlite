package vn.doancnpm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.Valid;

import vn.doancnpm.dto.Request.ToppingRequest;
import vn.doancnpm.dto.Response.ToppingResponse;
import vn.doancnpm.service.ToppingService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/toppings")
@RequiredArgsConstructor
public class ToppingController {

    private final ToppingService toppingService;
    
    @GetMapping
    public ResponseEntity<List<ToppingResponse>> getAllToppings(
            @RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(toppingService.getAllToppings(status));
    }

    // 1 Lấy chi tiết một topping theo ID
        @GetMapping("/{id}")
        public ResponseEntity<ToppingResponse> getToppingById(@PathVariable Long id) {
            return ResponseEntity.ok(toppingService.getToppingById(id));
        }
    // 2 Thêm mới Topping
    @PostMapping
    public ResponseEntity<ToppingResponse> createTopping(@Valid @RequestBody ToppingRequest request) {
        ToppingResponse created = toppingService.createTopping(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 3 Cập nhật tên và đơn giá Topping
    @PutMapping("/{id}")
    public ResponseEntity<ToppingResponse> updateTopping(
            @PathVariable Long id, 
            @Valid @RequestBody ToppingRequest request) {
        ToppingResponse updated = toppingService.updateTopping(id, request);
        return ResponseEntity.ok(updated);
    }
    // 4 cập nhật trạng thái
    @PatchMapping("/{id}/status")
        public ResponseEntity<ToppingResponse> updateToppingStatus(
                @PathVariable Long id,
                @RequestParam boolean status) {
            ToppingResponse updated = toppingService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        }

    /*
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
    ex.printStackTrace(); // In lỗi đỏ ra terminal IDE
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", 500);
    body.put("error", "Internal Server Error");
    body.put("message", ex.getMessage()); // Hiển thị nguyên nhân lỗi
    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
}  */    
     // ======================== Xử lý lỗi ========================
    // Spring luôn chọn handler "gần" exception nhất, nên các handler cụ thể bên dưới
    // được ưu tiên hơn handler Exception.class ở cuối.
 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        return buildError(HttpStatus.BAD_REQUEST, message);
    }
 
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Body JSON không hợp lệ");
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
 
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type phải là application/json");
    }
 
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        ex.printStackTrace(); // In lỗi ra terminal IDE
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
 
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
 
}
