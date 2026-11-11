package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.config.GlobalExceptionHandler;
import vn.doancnpm.controller.CustomerController;
import vn.doancnpm.dto.Request.CustomerRequest;
import vn.doancnpm.dto.Response.CustomerResponse;
import vn.doancnpm.service.CustomerService;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private static final String BASE_URL = "/api/v1/customers";

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper()
                .registerModule(new SimpleModule()
                        .addSerializer(LocalDate.class, ToStringSerializer.instance));
    }

    private CustomerResponse createCustomerResponse(Long id) {
        return new CustomerResponse(
                id,
                "customer1",
                "customer1@brewlite.vn",
                "Nguyen Van Khach",
                "0901234567",
                LocalDate.of(1996, 5, 20),
                true,
                150
        );
    }

    private CustomerRequest createValidCustomerRequest() {
        CustomerRequest request = new CustomerRequest();
        request.setFullName("Nguyen Van Khach Moi");
        request.setPhone("0987654321");
        request.setDateOfBirth(LocalDate.of(1996, 5, 20));
        request.setGender(true);
        return request;
    }

    // =========================================================================
    // 1. GET /api/v1/customers/profile
    // =========================================================================
    @Nested
    @DisplayName("GET " + BASE_URL + "/profile")
    class GetProfileTests {

        @Test
        @DisplayName("Có userId hợp lệ -> trả về 200 và thông tin khách hàng")
        void getMyProfile_validUserId_returnsCustomerProfile() throws Exception {
            CustomerResponse response = createCustomerResponse(1L);
            when(customerService.getMyProfile(1L)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/profile").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("customer1"))
                    .andExpect(jsonPath("$.email").value("customer1@brewlite.vn"))
                    .andExpect(jsonPath("$.fullName").value("Nguyen Van Khach"))
                    .andExpect(jsonPath("$.phone").value("0901234567"))
                    .andExpect(jsonPath("$.gender").value(true))
                    .andExpect(jsonPath("$.loyaltyPoints").value(150));

            verify(customerService).getMyProfile(1L);
        }

        @Test
        @DisplayName("Thiếu param userId -> trả về 400 Bad Request")
        void getMyProfile_missingUserId_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/profile"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("userId không phải là số -> trả về 400 Bad Request")
        void getMyProfile_nonNumericUserId_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/profile").param("userId", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("Không tìm thấy khách hàng -> trả về 404 Not Found")
        void getMyProfile_customerNotFound_returns404() throws Exception {
            when(customerService.getMyProfile(999L))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy thông tin khách hàng"));

            mockMvc.perform(get(BASE_URL + "/profile").param("userId", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy thông tin khách hàng"));

            verify(customerService).getMyProfile(999L);
        }
    }

    // =========================================================================
    // 2. PUT /api/v1/customers/profile
    // =========================================================================
    @Nested
    @DisplayName("PUT " + BASE_URL + "/profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Cập nhật thông tin thành công -> trả về 200 và thông tin mới")
        void updateMyProfile_validRequest_returnsUpdatedProfile() throws Exception {
            CustomerRequest request = createValidCustomerRequest();
            CustomerResponse response = new CustomerResponse(
                    1L,
                    "customer1",
                    "customer1@brewlite.vn",
                    request.getFullName(),
                    request.getPhone(),
                    request.getDateOfBirth(),
                    request.getGender(),
                    150
            );

            when(customerService.updateMyProfile(eq(1L), any(CustomerRequest.class))).thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/profile")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("Nguyen Van Khach Moi"))
                    .andExpect(jsonPath("$.phone").value("0987654321"));

            ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
            verify(customerService).updateMyProfile(eq(1L), captor.capture());
            assertThat(captor.getValue().getFullName()).isEqualTo("Nguyen Van Khach Moi");
            assertThat(captor.getValue().getPhone()).isEqualTo("0987654321");
        }

        @Test
        @DisplayName("Họ tên để trống -> validation lỗi trả về 400")
        void updateMyProfile_blankFullName_returns400() throws Exception {
            CustomerRequest request = createValidCustomerRequest();
            request.setFullName("");

            mockMvc.perform(put(BASE_URL + "/profile")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("Số điện thoại không đúng định dạng -> validation lỗi trả về 400")
        void updateMyProfile_invalidPhone_returns400() throws Exception {
            CustomerRequest request = createValidCustomerRequest();
            request.setPhone("12345");

            mockMvc.perform(put(BASE_URL + "/profile")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("Thiếu param userId -> trả về 400")
        void updateMyProfile_missingUserId_returns400() throws Exception {
            CustomerRequest request = createValidCustomerRequest();

            mockMvc.perform(put(BASE_URL + "/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("Khách hàng không tồn tại khi cập nhật -> trả về 404")
        void updateMyProfile_notFound_returns404() throws Exception {
            CustomerRequest request = createValidCustomerRequest();
            when(customerService.updateMyProfile(eq(999L), any(CustomerRequest.class)))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy thông tin khách hàng"));

            mockMvc.perform(put(BASE_URL + "/profile")
                    .param("userId", "999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy thông tin khách hàng"));

            verify(customerService).updateMyProfile(eq(999L), any(CustomerRequest.class));
        }
    }

    // =========================================================================
    // 3. GET /api/v1/customers/loyalty-history
    // =========================================================================
    @Nested
    @DisplayName("GET " + BASE_URL + "/loyalty-history")
    class GetLoyaltyHistoryTests {

        @Test
        @DisplayName("Lấy điểm tích lũy thành công -> trả về 200 và số điểm")
        void getMyLoyalPoint_validUserId_returnsPoints() throws Exception {
            when(customerService.getMyLoyalPoint(1L)).thenReturn(250.5f);

            mockMvc.perform(get(BASE_URL + "/loyalty-history").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("250.5"));

            verify(customerService).getMyLoyalPoint(1L);
        }

        @Test
        @DisplayName("Thiếu param userId -> trả về 400 Bad Request")
        void getMyLoyalPoint_missingUserId_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/loyalty-history"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("Khách hàng không tồn tại khi lấy điểm -> trả về 404")
        void getMyLoyalPoint_notFound_returns404() throws Exception {
            when(customerService.getMyLoyalPoint(999L))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy thông tin khách hàng"));

            mockMvc.perform(get(BASE_URL + "/loyalty-history").param("userId", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy thông tin khách hàng"));

            verify(customerService).getMyLoyalPoint(999L);
        }
    }
}