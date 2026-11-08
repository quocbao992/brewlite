package vn.doancnpm.service;

import java.math.BigDecimal;
import java.util.List;
import vn.doancnpm.dto.Request.PromotionRequest;
import vn.doancnpm.dto.Response.PromotionResponse;

public interface PromotionService {
    BigDecimal calculateDiscount(String code, BigDecimal subtotal);
    void markUsed(String code);
    PromotionResponse getByCode(String code);

    List<PromotionResponse> getAllPromotions();
    PromotionResponse getPromotionById(Long id);
    PromotionResponse createPromotion(PromotionRequest req);
    PromotionResponse updatePromotion(Long id, PromotionRequest req);
    void deletePromotion(Long id);
    PromotionResponse togglePromotionStatus(Long id);
}
