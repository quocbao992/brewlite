package vn.doancnpm.service.implement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.Promotion;
import vn.doancnpm.dto.Request.PromotionRequest;
import vn.doancnpm.dto.Response.PromotionResponse;
import vn.doancnpm.repository.PromotionRepository;
import vn.doancnpm.service.PromotionService;

@Service
@RequiredArgsConstructor
@Transactional
public class IPromotionService implements PromotionService {
    private final PromotionRepository repo;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal subtotal) {
        Promotion p = repo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));
        LocalDateTime now = LocalDateTime.now();

        if (!p.isActive() || now.isBefore(p.getStartDate()) || now.isAfter(p.getEndDate())) {
            throw new IllegalStateException("Mã khuyến mãi không còn hiệu lực");
        }
        if (p.getUsageLimit() != null && p.getUsedCount() >= p.getUsageLimit()) {
            throw new IllegalStateException("Mã khuyến mãi đã hết lượt dùng");
        }
        if (p.getMinOrderAmount() != null && subtotal.compareTo(p.getMinOrderAmount()) < 0) {
            throw new IllegalStateException("Đơn hàng chưa đạt giá trị tối thiểu " + p.getMinOrderAmount() + "đ");
        }

        BigDecimal discount = "PERCENT".equalsIgnoreCase(p.getDiscountType())
                ? subtotal.multiply(p.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                : p.getDiscountValue();

        if (p.getMaxDiscount() != null && discount.compareTo(p.getMaxDiscount()) > 0) {
            discount = p.getMaxDiscount();
        }
        return discount.min(subtotal);
    }

    @Override
    public void markUsed(String code) {
        Promotion p = repo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại"));
        p.setUsedCount(p.getUsedCount() + 1);
        repo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getAllPromotions() {
        return repo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        Promotion p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã khuyến mãi có ID: " + id));
        return mapToResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getByCode(String code) {
        Promotion p = repo.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã khuyến mãi: " + code));
        return mapToResponse(p);
    }

    @Override
    public PromotionResponse createPromotion(PromotionRequest req) {
        String cleanCode = req.getCode().trim().toUpperCase();
        if (repo.existsByCode(cleanCode)) {
            throw new IllegalArgumentException("Mã khuyến mãi \"" + cleanCode + "\" đã tồn tại trên hệ thống!");
        }

        LocalDateTime start = req.getStartDate() != null ? req.getStartDate() : LocalDateTime.now();
        LocalDateTime end = req.getEndDate() != null ? req.getEndDate() : start.plusMonths(1);

        Promotion p = Promotion.builder()
                .code(cleanCode)
                .description(req.getDescription())
                .discountType(req.getDiscountType().trim().toUpperCase())
                .discountValue(req.getDiscountValue())
                .minOrderAmount(req.getMinOrderAmount())
                .maxDiscount(req.getMaxDiscount())
                .startDate(start)
                .endDate(end)
                .usageLimit(req.getUsageLimit())
                .usedCount(0)
                .active(req.getActive() != null ? req.getActive() : true)
                .build();

        return mapToResponse(repo.save(p));
    }

    @Override
    public PromotionResponse updatePromotion(Long id, PromotionRequest req) {
        Promotion p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã khuyến mãi có ID: " + id));

        String cleanCode = req.getCode().trim().toUpperCase();
        if (!p.getCode().equalsIgnoreCase(cleanCode) && repo.existsByCode(cleanCode)) {
            throw new IllegalArgumentException("Mã khuyến mãi \"" + cleanCode + "\" đã trùng với một mã khác!");
        }

        p.setCode(cleanCode);
        p.setDescription(req.getDescription());
        p.setDiscountType(req.getDiscountType().trim().toUpperCase());
        p.setDiscountValue(req.getDiscountValue());
        p.setMinOrderAmount(req.getMinOrderAmount());
        p.setMaxDiscount(req.getMaxDiscount());
        if (req.getStartDate() != null) p.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) p.setEndDate(req.getEndDate());
        p.setUsageLimit(req.getUsageLimit());
        if (req.getActive() != null) p.setActive(req.getActive());

        return mapToResponse(repo.save(p));
    }

    @Override
    public void deletePromotion(Long id) {
        Promotion p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã khuyến mãi có ID: " + id));
        repo.delete(p);
    }

    @Override
    public PromotionResponse togglePromotionStatus(Long id) {
        Promotion p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy mã khuyến mãi có ID: " + id));
        p.setActive(!p.isActive());
        return mapToResponse(repo.save(p));
    }

    private PromotionResponse mapToResponse(Promotion p) {
        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscount(p.getMaxDiscount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .usageLimit(p.getUsageLimit())
                .usedCount(p.getUsedCount())
                .active(p.isActive())
                .build();
    }
}
