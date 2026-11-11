package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.domain.Promotion;
import vn.doancnpm.dto.Request.PromotionRequest;
import vn.doancnpm.dto.Response.PromotionResponse;
import vn.doancnpm.repository.PromotionRepository;
import vn.doancnpm.service.implement.IPromotionService;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock PromotionRepository repo;
    @InjectMocks IPromotionService service;

    // GIAM10: giảm 10%, tối thiểu 50.000, tối đa 30.000, đang hiệu lực, không giới hạn lượt
    private Promotion.PromotionBuilder giam10() {
        return Promotion.builder()
                .id(1L)
                .code("GIAM10")
                .discountType("PERCENT")
                .discountValue(new BigDecimal("10"))
                .minOrderAmount(new BigDecimal("50000"))
                .maxDiscount(new BigDecimal("30000"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .usedCount(0)
                .active(true);
    }

    private void found(Promotion p) {
        when(repo.findByCode(p.getCode())).thenReturn(Optional.of(p));
    }

    // ===== calculateDiscount =====

    @Test
    void calculateDiscount_percent() {
        found(giam10().build());
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("200000"))).isEqualByComparingTo("20000");
    }

    @Test
    void calculateDiscount_percentIsCappedByMaxDiscount() {
        found(giam10().build());
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("500000"))).isEqualByComparingTo("30000");
    }

    @Test
    void calculateDiscount_percentRoundsDown() {
        found(giam10().discountValue(new BigDecimal("15")).minOrderAmount(null).maxDiscount(null).build());
        // 15% của 33.333 = 4.999,95 -> 4.999
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("33333"))).isEqualByComparingTo("4999");
    }

    @Test
    void calculateDiscount_fixedAmount() {
        found(giam10().discountType("FIXED").discountValue(new BigDecimal("20000")).maxDiscount(null).build());
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("90000"))).isEqualByComparingTo("20000");
    }

    @Test
    void calculateDiscount_neverExceedsSubtotal() {
        found(giam10().discountType("FIXED").discountValue(new BigDecimal("100000"))
                .minOrderAmount(null).maxDiscount(null).build());
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("60000"))).isEqualByComparingTo("60000");
    }

    @Test
    void calculateDiscount_codeNotFound_throws() {
        when(repo.findByCode("XYZ")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.calculateDiscount("XYZ", new BigDecimal("100000")));
    }

    @Test
    void calculateDiscount_belowMinOrder_throws() {
        found(giam10().build());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.calculateDiscount("GIAM10", new BigDecimal("40000")));
        assertThat(ex.getMessage()).contains("tối thiểu");
    }

    @Test
    void calculateDiscount_exactlyMinOrder_ok() {
        found(giam10().build());
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("50000"))).isEqualByComparingTo("5000");
    }

    @Test
    void calculateDiscount_expired_throws() {
        found(giam10().startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1)).build());
        assertThrows(IllegalStateException.class, () -> service.calculateDiscount("GIAM10", new BigDecimal("200000")));
    }

    @Test
    void calculateDiscount_notStartedYet_throws() {
        found(giam10().startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5)).build());
        assertThrows(IllegalStateException.class, () -> service.calculateDiscount("GIAM10", new BigDecimal("200000")));
    }

    @Test
    void calculateDiscount_inactive_throws() {
        found(giam10().active(false).build());
        assertThrows(IllegalStateException.class, () -> service.calculateDiscount("GIAM10", new BigDecimal("200000")));
    }

    @Test
    void calculateDiscount_usageLimitReached_throws() {
        found(giam10().usageLimit(1).usedCount(1).build());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.calculateDiscount("GIAM10", new BigDecimal("200000")));
        assertThat(ex.getMessage()).contains("hết lượt");
    }

    @Test
    void calculateDiscount_usageLimitNotReached_ok() {
        found(giam10().usageLimit(5).usedCount(4).build());
        assertThat(service.calculateDiscount("GIAM10", new BigDecimal("200000"))).isEqualByComparingTo("20000");
    }

    // ===== markUsed =====

    @Test
    void markUsed_increasesUsedCount() {
        Promotion p = giam10().usedCount(2).build();
        found(p);

        service.markUsed("GIAM10");

        ArgumentCaptor<Promotion> cap = ArgumentCaptor.forClass(Promotion.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getUsedCount()).isEqualTo(3);
    }

    @Test
    void markUsed_codeNotFound_throws() {
        when(repo.findByCode("XYZ")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.markUsed("XYZ"));
        verify(repo, never()).save(any(Promotion.class));
    }

    // ===== đọc =====

    @Test
    void getAllPromotions_returnsMappedList() {
        when(repo.findAll()).thenReturn(List.of(giam10().build()));
        List<PromotionResponse> list = service.getAllPromotions();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCode()).isEqualTo("GIAM10");
    }

    @Test
    void getPromotionById_found() {
        when(repo.findById(1L)).thenReturn(Optional.of(giam10().build()));
        assertThat(service.getPromotionById(1L).getCode()).isEqualTo("GIAM10");
    }

    @Test
    void getPromotionById_notFound_throws() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.getPromotionById(9L));
    }

    // ===== createPromotion =====

    @Test
    void createPromotion_normalizesCodeAndAppliesDefaults() {
        when(repo.existsByCode("GIAM10")).thenReturn(false);
        when(repo.save(any(Promotion.class))).thenAnswer(i -> i.getArgument(0));
        PromotionRequest req = new PromotionRequest();
        req.setCode("  giam10 ");
        req.setDiscountType("percent");
        req.setDiscountValue(new BigDecimal("10"));

        service.createPromotion(req);

        ArgumentCaptor<Promotion> cap = ArgumentCaptor.forClass(Promotion.class);
        verify(repo).save(cap.capture());
        Promotion saved = cap.getValue();
        assertThat(saved.getCode()).isEqualTo("GIAM10");
        assertThat(saved.getDiscountType()).isEqualTo("PERCENT");
        assertThat(saved.getUsedCount()).isEqualTo(0);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getEndDate()).isAfter(saved.getStartDate());
    }

    @Test
    void createPromotion_duplicateCode_throws() {
        when(repo.existsByCode("GIAM10")).thenReturn(true);
        PromotionRequest req = new PromotionRequest();
        req.setCode("giam10");
        req.setDiscountType("PERCENT");

        assertThrows(IllegalArgumentException.class, () -> service.createPromotion(req));
        verify(repo, never()).save(any(Promotion.class));
    }

    // ===== updatePromotion =====

    @Test
    void updatePromotion_notFound_throws() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        PromotionRequest req = new PromotionRequest();
        req.setCode("GIAM10");
        req.setDiscountType("PERCENT");
        assertThrows(EntityNotFoundException.class, () -> service.updatePromotion(9L, req));
    }

    @Test
    void updatePromotion_codeTakenByAnother_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(giam10().build()));
        when(repo.existsByCode("GIAM20K")).thenReturn(true);
        PromotionRequest req = new PromotionRequest();
        req.setCode("giam20k");
        req.setDiscountType("FIXED");

        assertThrows(IllegalArgumentException.class, () -> service.updatePromotion(1L, req));
        verify(repo, never()).save(any(Promotion.class));
    }

    @Test
    void updatePromotion_success_updatesFields() {
        Promotion p = giam10().build();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any(Promotion.class))).thenAnswer(i -> i.getArgument(0));
        PromotionRequest req = new PromotionRequest();
        req.setCode("GIAM10");                                   // giữ nguyên mã, không cần kiểm tra trùng
        req.setDiscountType("fixed");
        req.setDiscountValue(new BigDecimal("20000"));

        service.updatePromotion(1L, req);

        assertThat(p.getDiscountType()).isEqualTo("FIXED");
        assertThat(p.getDiscountValue()).isEqualByComparingTo("20000");
        verify(repo, never()).existsByCode(any());
    }

    // ===== delete / toggle =====

    @Test
    void deletePromotion_success() {
        Promotion p = giam10().build();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        service.deletePromotion(1L);
        verify(repo).delete(p);
    }

    @Test
    void deletePromotion_notFound_throws() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.deletePromotion(9L));
        verify(repo, never()).delete(any(Promotion.class));
    }

    @Test
    void togglePromotionStatus_flipsActive() {
        Promotion p = giam10().active(true).build();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any(Promotion.class))).thenAnswer(i -> i.getArgument(0));

        service.togglePromotionStatus(1L);
        assertThat(p.isActive()).isFalse();

        service.togglePromotionStatus(1L);
        assertThat(p.isActive()).isTrue();
    }
}