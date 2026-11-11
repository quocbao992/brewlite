package vn.doancnpm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.doancnpm.dto.Request.PromotionRequest;
import vn.doancnpm.dto.Response.PromotionResponse;
import vn.doancnpm.service.PromotionService;
import vn.doancnpm.controller.PromotionController;

@ExtendWith(MockitoExtension.class)
class PromotionControllerTest {

    @Mock PromotionService promotionService;
    MockMvc mvc;

    private static final String BODY =
            "{\"code\":\"GIAM10\",\"description\":\"Giảm 10%\",\"discountType\":\"PERCENT\","
          + "\"discountValue\":10,\"minOrderAmount\":50000,\"maxDiscount\":30000,"
          + "\"usageLimit\":100,\"active\":true}";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PromotionController(promotionService)).build();
    }

    private PromotionResponse promo() {
        return PromotionResponse.builder()
                .id(1L).code("GIAM10").discountType("PERCENT")
                .discountValue(new BigDecimal("10"))
                .usedCount(0).active(true)
                .build();
    }

    // ===== GET /validate =====

    @Test
    void validate_returnsDiscountAmount() throws Exception {
        when(promotionService.calculateDiscount("GIAM10", new BigDecimal("200000")))
                .thenReturn(new BigDecimal("20000"));

        mvc.perform(get("/api/promotions/validate").param("code", "GIAM10").param("amount", "200000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(20000));
    }

    @Test
    void validate_missingCode_returns400() throws Exception {
        mvc.perform(get("/api/promotions/validate").param("amount", "200000"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void validate_amountNotNumber_returns400() throws Exception {
        mvc.perform(get("/api/promotions/validate").param("code", "GIAM10").param("amount", "abc"))
            .andExpect(status().isBadRequest());
    }

    // ===== GET =====

    @Test
    void getAll_returnsList() throws Exception {
        when(promotionService.getAllPromotions()).thenReturn(List.of(promo()));

        mvc.perform(get("/api/promotions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("GIAM10"));
    }

    @Test
    void getById_returnsPromotion() throws Exception {
        when(promotionService.getPromotionById(1L)).thenReturn(promo());

        mvc.perform(get("/api/promotions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("GIAM10"));
    }

    // ===== POST / PUT / DELETE / PATCH =====

    @Test
    void create_returns201() throws Exception {
        when(promotionService.createPromotion(any(PromotionRequest.class))).thenReturn(promo());

        mvc.perform(post("/api/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("GIAM10"));
    }

    @Test
    void create_withoutBody_returns400() throws Exception {
        mvc.perform(post("/api/promotions").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200() throws Exception {
        when(promotionService.updatePromotion(any(Long.class), any(PromotionRequest.class))).thenReturn(promo());

        mvc.perform(put("/api/promotions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_returnsMessage() throws Exception {
        mvc.perform(delete("/api/promotions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
        verify(promotionService).deletePromotion(1L);
    }

    @Test
    void toggleStatus_returns200() throws Exception {
        when(promotionService.togglePromotionStatus(1L)).thenReturn(promo());

        mvc.perform(patch("/api/promotions/1/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }
}