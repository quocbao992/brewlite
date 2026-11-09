package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.doancnpm.controller.CartController;
import vn.doancnpm.domain.CartItem;
import vn.doancnpm.dto.Request.CartItemRequest;
import vn.doancnpm.dto.Request.CartRequest;
import vn.doancnpm.dto.Response.CartResponse;
import vn.doancnpm.service.CartService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private CartService cartService;

  @InjectMocks
  private CartController cartController;

  @BeforeEach
  void setUp() {

    mockMvc = MockMvcBuilders.standaloneSetup(cartController)
        .setMessageConverters(
            new StringHttpMessageConverter(StandardCharsets.UTF_8),
            new MappingJackson2HttpMessageConverter(objectMapper))
        .build();
  }

  // ==========================================
  // 1. GET /api/v1/cart
  // ==========================================
  @Nested
  @DisplayName("GET /api/v1/cart")
  class GetCartEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK cùng dữ liệu giỏ hàng")
    void getCart_ReturnsOk() throws Exception {
      Long customerId = 1L;
      CartResponse mockResponse = CartResponse.builder()
          .id(10L)
          .customerId(customerId)
          .totalItems(2)
          .totalPrice(new BigDecimal("60000"))
          .items(Collections.emptyList())
          .build();

      when(cartService.getCart(customerId)).thenReturn(mockResponse);

      mockMvc
          .perform(
              get("/api/v1/cart")
                  .param("customerId", String.valueOf(customerId))
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(10L))
          .andExpect(jsonPath("$.customerId").value(1L))
          .andExpect(jsonPath("$.totalItems").value(2))
          .andExpect(jsonPath("$.totalPrice").value(60000));

      verify(cartService, times(1)).getCart(customerId);
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi thiếu customerId")
    void getCart_MissingParam_ReturnsBadRequest() throws Exception {
      mockMvc
          .perform(get("/api/v1/cart").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());

      verify(cartService, never()).getCart(any());
    }
  }

  // ==========================================
  // 2. POST /api/v1/cart/items
  // ==========================================
  @Nested
  @DisplayName("POST /api/v1/cart/items")
  class AddToCartEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 201 Created và thông báo thành công")
    void addToCart_ReturnsCreated() throws Exception {
      CartItemRequest itemRequest = CartItemRequest.builder()
          .productId(100L)
          .quantity(1)
          .size(CartItem.Size.M)
          .toppings(Collections.emptyList())
          .build();

      CartRequest cartRequest = CartRequest.builder().customerId(1L).item(itemRequest).build();

      when(cartService.addToCart(any(CartRequest.class)))
          .thenReturn(CartResponse.builder().build());

      mockMvc
          .perform(
              post("/api/v1/cart/items")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(cartRequest)))
          .andExpect(status().isCreated())
          .andExpect(
              result -> {
                String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(body).isEqualTo("Thêm vào giỏ hàng thành công !");
              });

      verify(cartService, times(1)).addToCart(any(CartRequest.class));
    }
  }

  // ==========================================
  // 3. PUT /api/v1/cart/items/{cartItemId}
  // ==========================================
  @Nested
  @DisplayName("PUT /api/v1/cart/items/{cartItemId}")
  class UpdateCartItemEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi cập nhật số lượng thành công")
    void updateCartItem_ReturnsOk() throws Exception {
      Long cartItemId = 5L;
      CartController.UQRequest request = new CartController.UQRequest(3);

      when(cartService.updateCartItem(eq(cartItemId), eq(3)))
          .thenReturn(CartResponse.builder().build());

      mockMvc
          .perform(
              put("/api/v1/cart/items/{cartItemId}", cartItemId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(
              result -> {
                String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(body).isEqualTo("Cập nhật số lượng thành công !");
              });

      verify(cartService, times(1)).updateCartItem(cartItemId, 3);
    }
  }

  // ==========================================
  // 4. DELETE /api/v1/cart/items/{cartItemId}
  // ==========================================
  @Nested
  @DisplayName("DELETE /api/v1/cart/items/{cartItemId}")
  class RemoveCartItemEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi xóa món thành công")
    void removeCartItem_ReturnsOk() throws Exception {
      Long cartItemId = 5L;

      doNothing().when(cartService).removeCartItem(cartItemId);

      mockMvc
          .perform(delete("/api/v1/cart/items/{cartItemId}", cartItemId))
          .andExpect(status().isOk())
          .andExpect(
              result -> {
                String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(body).isEqualTo("Xóa món khỏi giỏ hàng thành công !");
              });

      verify(cartService, times(1)).removeCartItem(cartItemId);
    }
  }

  // ==========================================
  // 5. DELETE /api/v1/cart
  // ==========================================
  @Nested
  @DisplayName("DELETE /api/v1/cart")
  class ClearCartEndpointTests {

    @Test
    @DisplayName("Trả về HTTP 200 OK khi làm trống giỏ hàng")
    void clearCart_ReturnsOk() throws Exception {
      Long customerId = 1L;

      doNothing().when(cartService).clearCart(customerId);

      mockMvc
          .perform(delete("/api/v1/cart").param("customerId", String.valueOf(customerId)))
          .andExpect(status().isOk())
          .andExpect(
              result -> {
                String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                assertThat(body).isEqualTo("Đã làm trống giỏ hàng !");
              });

      verify(cartService, times(1)).clearCart(customerId);
    }

    @Test
    @DisplayName("Trả về HTTP 400 Bad Request khi thiếu param customerId")
    void clearCart_MissingParam_ReturnsBadRequest() throws Exception {
      mockMvc.perform(delete("/api/v1/cart")).andExpect(status().isBadRequest());

      verify(cartService, never()).clearCart(any());
    }
  }
}
