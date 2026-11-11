package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import vn.doancnpm.controller.UserController;
import vn.doancnpm.dto.Request.ChangePasswordRequest;
import vn.doancnpm.dto.Request.UpdateProfileRequest;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String BASE_URL = "/api/v1/users";

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper()
                .registerModule(new SimpleModule()
                        .addSerializer(LocalDate.class, ToStringSerializer.instance));
    }

    private UserResponse createUserResponse(Long id) {
        return UserResponse.builder()
                .id(id)
                .username("testuser")
                .fullName("Nguyen Van Test")
                .email("testuser@brewlite.vn")
                .phone("0901234567")
                .role("CUSTOMER")
                .status(true)
                .dateOfBirth(LocalDate.of(1998, 8, 20))
                .gender(true)
                .build();
    }

    private UpdateProfileRequest createValidUpdateProfileRequest() {
        return UpdateProfileRequest.builder()
                .fullName("Nguyen Van Cap Nhat")
                .phone("0901234567")
                .dateOfBirth(LocalDate.of(1998, 8, 20))
                .gender(true)
                .build();
    }

    private ChangePasswordRequest createValidChangePasswordRequest() {
        return ChangePasswordRequest.builder()
                .oldPassword("OldPass123@")
                .newPassword("NewPass123@")
                .confirmPassword("NewPass123@")
                .build();
    }

    // =========================================================================
    // 1. GET /api/v1/users/me
    // =========================================================================
    @Nested
    @DisplayName("GET " + BASE_URL + "/me")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("Lấy thông tin cá nhân thành công -> trả về 200 và thông tin UserResponse")
        void getCurrentUserProfile_validUserId_returnsProfile() throws Exception {
            UserResponse response = createUserResponse(1L);
            when(userService.getCurrentUserProfile(1L)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/me").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.fullName").value("Nguyen Van Test"))
                    .andExpect(jsonPath("$.email").value("testuser@brewlite.vn"))
                    .andExpect(jsonPath("$.phone").value("0901234567"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.status").value(true));

            verify(userService).getCurrentUserProfile(1L);
        }

        @Test
        @DisplayName("Thiếu param userId -> trả về 400 Bad Request")
        void getCurrentUserProfile_missingUserId_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("userId không phải số -> trả về 400 Bad Request")
        void getCurrentUserProfile_nonNumericUserId_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me").param("userId", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Không tìm thấy người dùng -> trả về 404 Not Found")
        void getCurrentUserProfile_userNotFound_returns404() throws Exception {
            when(userService.getCurrentUserProfile(999L))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"));

            mockMvc.perform(get(BASE_URL + "/me").param("userId", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(userService).getCurrentUserProfile(999L);
        }
    }

    // =========================================================================
    // 2. PUT /api/v1/users/me
    // =========================================================================
    @Nested
    @DisplayName("PUT " + BASE_URL + "/me")
    class UpdateCurrentUserProfileTests {

        @Test
        @DisplayName("Cập nhật thông tin hợp lệ -> trả về 200 và thông tin đã cập nhật")
        void updateCurrentUserProfile_validRequest_returnsUpdatedProfile() throws Exception {
            UpdateProfileRequest request = createValidUpdateProfileRequest();
            UserResponse response = createUserResponse(1L);
            response.setFullName("Nguyen Van Cap Nhat");

            when(userService.updateCurrentUserProfile(eq(1L), any(UpdateProfileRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/me")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("Nguyen Van Cap Nhat"));

            ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
            verify(userService).updateCurrentUserProfile(eq(1L), captor.capture());
            assertThat(captor.getValue().getFullName()).isEqualTo("Nguyen Van Cap Nhat");
            assertThat(captor.getValue().getPhone()).isEqualTo("0901234567");
        }

        @Test
        @DisplayName("Validation thất bại: họ và tên để trống -> trả về 400")
        void updateCurrentUserProfile_blankFullName_returns400() throws Exception {
            UpdateProfileRequest request = createValidUpdateProfileRequest();
            request.setFullName("");

            mockMvc.perform(put(BASE_URL + "/me")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Validation thất bại: số điện thoại không hợp lệ -> trả về 400")
        void updateCurrentUserProfile_invalidPhone_returns400() throws Exception {
            UpdateProfileRequest request = createValidUpdateProfileRequest();
            request.setPhone("012345");

            mockMvc.perform(put(BASE_URL + "/me")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Thiếu param userId khi cập nhật profile -> trả về 400")
        void updateCurrentUserProfile_missingUserId_returns400() throws Exception {
            UpdateProfileRequest request = createValidUpdateProfileRequest();

            mockMvc.perform(put(BASE_URL + "/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("User không tồn tại khi cập nhật profile -> trả về 404")
        void updateCurrentUserProfile_userNotFound_returns404() throws Exception {
            UpdateProfileRequest request = createValidUpdateProfileRequest();
            when(userService.updateCurrentUserProfile(eq(999L), any(UpdateProfileRequest.class)))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"));

            mockMvc.perform(put(BASE_URL + "/me")
                    .param("userId", "999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(userService).updateCurrentUserProfile(eq(999L), any(UpdateProfileRequest.class));
        }
    }

    // =========================================================================
    // 3. PUT /api/v1/users/change-password & PUT /api/v1/users/me/change-password
    // =========================================================================
    @Nested
    @DisplayName("PUT " + BASE_URL + "/change-password and /me/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Đổi mật khẩu thành công qua endpoint /change-password -> trả về 200")
        void changePassword_success_changePasswordEndpoint() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            doNothing().when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

            verify(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("Đổi mật khẩu thành công qua alias endpoint /me/change-password -> trả về 200")
        void changePassword_success_meChangePasswordEndpoint() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            doNothing().when(userService).changePassword(eq(2L), any(ChangePasswordRequest.class));

            mockMvc.perform(put(BASE_URL + "/me/change-password")
                    .param("userId", "2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

            verify(userService).changePassword(eq(2L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("Mật khẩu cũ để trống -> validation lỗi trả về 400")
        void changePassword_blankOldPassword_returns400() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            request.setOldPassword("");

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Mật khẩu mới ngắn hơn 8 ký tự -> validation lỗi trả về 400")
        void changePassword_shortNewPassword_returns400() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            request.setNewPassword("short");

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Xác nhận mật khẩu để trống -> validation lỗi trả về 400")
        void changePassword_blankConfirmPassword_returns400() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            request.setConfirmPassword("");

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Service ném IllegalArgumentException (sai mật khẩu cũ) -> trả về 400")
        void changePassword_wrongOldPassword_returns400() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            doThrow(new IllegalArgumentException("Mật khẩu cũ không chính xác"))
                    .when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .param("userId", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Mật khẩu cũ không chính xác"));

            verify(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("User không tồn tại khi đổi mật khẩu -> trả về 404")
        void changePassword_userNotFound_returns404() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();
            doThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"))
                    .when(userService).changePassword(eq(999L), any(ChangePasswordRequest.class));

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .param("userId", "999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(userService).changePassword(eq(999L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("Thiếu param userId khi đổi mật khẩu -> trả về 400")
        void changePassword_missingUserId_returns400() throws Exception {
            ChangePasswordRequest request = createValidChangePasswordRequest();

            mockMvc.perform(put(BASE_URL + "/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }
}

