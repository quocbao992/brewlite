package vn.doancnpm.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.dto.Request.PromotionRequest;
import vn.doancnpm.dto.Response.PromotionResponse;
import vn.doancnpm.service.PromotionService;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    // GET /api/promotions/validate?code=GIAM10&amount=200000  -> số tiền được giảm
    @GetMapping("/validate")
    public ResponseEntity<BigDecimal> validate(@RequestParam String code,
                                               @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(promotionService.calculateDiscount(code, amount));
    }

    // GET /api/promotions -> danh sách khuyến mãi
    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAll() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    // GET /api/promotions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    // POST /api/promotions -> thêm mới
    @PostMapping
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.createPromotion(req));
    }

    // PUT /api/promotions/{id} -> cập nhật
    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable Long id, @Valid @RequestBody PromotionRequest req) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, req));
    }

    // DELETE /api/promotions/{id} -> xóa
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa mã khuyến mãi thành công"));
    }

    // PATCH /api/promotions/{id}/status -> bật / tắt
    @PatchMapping("/{id}/status")
    public ResponseEntity<PromotionResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.togglePromotionStatus(id));
    }
}
