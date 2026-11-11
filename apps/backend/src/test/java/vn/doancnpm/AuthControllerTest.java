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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.config.GlobalExceptionHandler;
import vn.doancnpm.controller.AuthController;
import vn.doancnpm.dto.Request.ForgotPasswordRequest;
import vn.doancnpm.dto.Request.LoginRequest;
import vn.doancnpm.dto.Request.LogoutRequest;
import vn.doancnpm.dto.Request.RefreshTokenRequest;
import vn.doancnpm.dto.Request.RegisterRequest;
import vn.doancnpm.dto.Request.ResetPasswordRequest;
import vn.doancnpm.dto.Response.ForgotPasswordResponse;
import vn.doancnpm.dto.Response.LoginResponse;
import vn.doancnpm.dto.Response.RefreshTokenResponse;
import vn.doancnpm.dto.Response.RegisterResponse;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String BASE_URL = "/api/v1/auth";

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper()
                .registerModule(new SimpleModule()
                        .addSerializer(LocalDate.class, ToStringSerializer.instance));
    }

    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser");
        req.setPassword("Password123@");
        req.setFullName("Nguyen Van A");
        req.setEmail("validuser@example.com");
        req.setPhone("0901234567");
        req.setDateOfBirth(LocalDate.of(2000, 1, 1));
        req.setGender(true);
        return req;
    }

    private LoginRequest createValidLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setUsername("validuser");
        req.setPassword("Password123@");
        return req;
    }

    // =========================================================================
    // 1. POST /api/v1/auth/register
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL + "/register")
    class RegisterTests {

        @Test
        @DisplayName("Đăng ký thành công -> trả về 200 và thông tin tài khoản đã tạo")
        void register_validRequest_returnsRegisterResponse() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            RegisterResponse response = RegisterResponse.builder()
                    .id(1L)
                    .username("validuser")
                    .fullName("Nguyen Van A")
                    .email("validuser@example.com")
                    .role("CUSTOMER")
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("validuser"))
                    .andExpect(jsonPath("$.email").value("validuser@example.com"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Đăng ký thất bại khi username rỗng -> trả về 400 Bad Request")
        void register_blankUsername_returns400() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setUsername("");

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Đăng ký thất bại khi password quá yếu -> trả về 400 Bad Request")
        void register_weakPassword_returns400() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPassword("123456");

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Đăng ký thất bại khi email sai định dạng -> trả về 400 Bad Request")
        void register_invalidEmail_returns400() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setEmail("invalid-email-format");

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Đăng ký thất bại khi phone sai format -> trả về 400 Bad Request")
        void register_invalidPhone_returns400() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            request.setPhone("12345");

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Service ném IllegalArgumentException (username đã tồn tại) -> trả về 400")
        void register_serviceThrowsIllegalArgument_returns400() throws Exception {
            RegisterRequest request = createValidRegisterRequest();
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("Tên đăng nhập đã được sử dụng"));

            mockMvc.perform(post(BASE_URL + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Tên đăng nhập đã được sử dụng"));

            verify(authService).register(any(RegisterRequest.class));
        }
    }

    // =========================================================================
    // 2. POST /api/v1/auth/login
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL + "/login")
    class LoginTests {

        @Test
        @DisplayName("Đăng nhập thành công -> trả về 200 kèm token và thông tin người dùng")
        void login_validCredentials_returnsLoginResponse() throws Exception {
            LoginRequest request = createValidLoginRequest();
            UserResponse userResponse = UserResponse.builder()
                    .id(1L)
                    .username("validuser")
                    .fullName("Nguyen Van A")
                    .email("validuser@example.com")
                    .role("CUSTOMER")
                    .build();

            LoginResponse response = LoginResponse.builder()
                    .token("mock-jwt-access-token")
                    .refreshToken("mock-jwt-refresh-token")
                    .expiresIn(3600L)
                    .user(userResponse)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("mock-jwt-refresh-token"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.user.username").value("validuser"))
                    .andExpect(jsonPath("$.user.role").value("CUSTOMER"));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Đăng nhập với username rỗng -> trả về 400 Bad Request")
        void login_blankUsername_returns400() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("");
            request.setPassword("Password123@");

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Đăng nhập với password rỗng -> trả về 400 Bad Request")
        void login_blankPassword_returns400() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("validuser");
            request.setPassword("");

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Sai mật khẩu (BadCredentialsException) -> trả về 401 Unauthorized")
        void login_badCredentials_returns401() throws Exception {
            LoginRequest request = createValidLoginRequest();
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác"));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Tài khoản bị khóa (DisabledException) -> trả về 403 Forbidden")
        void login_accountDisabled_returns403() throws Exception {
            LoginRequest request = createValidLoginRequest();
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new DisabledException("Tài khoản của bạn đã bị khóa"));

            mockMvc.perform(post(BASE_URL + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("Tài khoản của bạn đã bị khóa"));

            verify(authService).login(any(LoginRequest.class));
        }
    }

    // =========================================================================
    // 3. POST /api/v1/auth/logout
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL + "/logout")
    class LogoutTests {

        @Test
        @DisplayName("Đăng xuất có Bearer token và refresh token -> gọi service với cả 2 token")
        void logout_withBearerHeaderAndRefreshToken_success() throws Exception {
            LogoutRequest body = new LogoutRequest();
            body.setRefreshToken("mock-refresh-token");

            mockMvc.perform(post(BASE_URL + "/logout")
                    .header("Authorization", "Bearer mock-access-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

            verify(authService).logout("mock-access-token", "mock-refresh-token");
        }

        @Test
        @DisplayName("Đăng xuất chỉ có Bearer token, không có body -> gọi service với accessToken và null refreshToken")
        void logout_withBearerHeaderOnly_success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout")
                    .header("Authorization", "Bearer mock-access-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

            verify(authService).logout("mock-access-token", null);
        }

        @Test
        @DisplayName("Đăng xuất với header không phải Bearer -> accessToken là null")
        void logout_nonBearerHeader_passesNullAccessToken() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout")
                    .header("Authorization", "Basic dXNlcjpwYXNz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

            verify(authService).logout(null, null);
        }

        @Test
        @DisplayName("Đăng xuất không có header Authorization và không có body -> gọi service với null, null")
        void logout_noHeaderNoBody_success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

            verify(authService).logout(null, null);
        }
    }

    // =========================================================================
    // 4. POST /api/v1/auth/refresh-token
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL + "/refresh-token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh token hợp lệ -> trả về token mới thành công")
        void refreshToken_valid_returnsNewToken() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .token("new-access-token")
                    .refreshToken("new-refresh-token")
                    .expiresIn(3600L)
                    .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));

            verify(authService).refreshToken(any(RefreshTokenRequest.class));
        }

        @Test
        @DisplayName("RefreshToken rỗng -> validation thất bại trả về 400")
        void refreshToken_blank_returns400() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("");

            mockMvc.perform(post(BASE_URL + "/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("RefreshToken hết hạn hoặc không hợp lệ -> service trả 400")
        void refreshToken_invalid_returns400() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");
            when(authService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenThrow(new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn"));

            mockMvc.perform(post(BASE_URL + "/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Refresh token không hợp lệ hoặc đã hết hạn"));

            verify(authService).refreshToken(any(RefreshTokenRequest.class));
        }
    }

    // =========================================================================
    // 5. POST /api/v1/auth/forgot-password
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL + "/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Email hợp lệ -> gửi mã OTP thành công trả về 200")
        void forgotPassword_validEmail_returnsSuccess() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("user@example.com");

            ForgotPasswordResponse response = new ForgotPasswordResponse("Mã xác nhận đã được gửi", "mock-reset-token");
            when(authService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Mã xác nhận đã được gửi"))
                    .andExpect(jsonPath("$.resetToken").value("mock-reset-token"));

            verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
        }

        @Test
        @DisplayName("Email không đúng định dạng -> trả về 400 Bad Request")
        void forgotPassword_invalidEmailFormat_returns400() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("not-an-email");

            mockMvc.perform(post(BASE_URL + "/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Email không tồn tại trong hệ thống -> trả về 404 Not Found")
        void forgotPassword_emailNotFound_returns404() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("notfound@example.com");

            when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với email: notfound@example.com"));

            mockMvc.perform(post(BASE_URL + "/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với email: notfound@example.com"));

            verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
        }
    }

    // =========================================================================
    // 6. POST /api/v1/auth/reset-password
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL + "/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Đặt lại mật khẩu thành công -> trả về 200 kèm thông báo")
        void resetPassword_validRequest_returnsSuccessMessage() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setEmail("user@example.com");
            request.setOtpToken("123456");
            request.setNewPassword("NewPassword123@");

            doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post(BASE_URL + "/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));

            ArgumentCaptor<ResetPasswordRequest> captor = ArgumentCaptor.forClass(ResetPasswordRequest.class);
            verify(authService).resetPassword(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
            assertThat(captor.getValue().getOtpToken()).isEqualTo("123456");
        }

        @Test
        @DisplayName("Thiếu OTP token -> validation thất bại trả về 400")
        void resetPassword_missingOtp_returns400() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setEmail("user@example.com");
            request.setOtpToken("");
            request.setNewPassword("NewPassword123@");

            mockMvc.perform(post(BASE_URL + "/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("OTP không hợp lệ hoặc hết hạn -> trả về 400")
        void resetPassword_invalidOtp_returns400() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setEmail("user@example.com");
            request.setOtpToken("wrong-otp");
            request.setNewPassword("NewPassword123@");

            doThrow(new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn"))
                    .when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post(BASE_URL + "/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Mã OTP không chính xác hoặc đã hết hạn"));

            verify(authService).resetPassword(any(ResetPasswordRequest.class));
        }
    }
}
