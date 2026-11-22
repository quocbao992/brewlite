package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.ForgotPasswordRequest;
import vn.doancnpm.dto.Request.ResetPasswordRequest;
import vn.doancnpm.dto.Request.VerifyOtpRequest;
import vn.doancnpm.dto.Response.ForgotPasswordResponse;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.UserRepository;
import vn.doancnpm.service.JWTUtils;
import vn.doancnpm.service.implement.IAuthService;

@ExtendWith(MockitoExtension.class)
class IAuthServiceOtpTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private JWTUtils jwtUtils;

    private IAuthService authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("No local redis"));
        authService = new IAuthService(
                userRepository,
                customerRepository,
                passwordEncoder,
                redisTemplate,
                jwtUtils
        );
    }

    @Test
    @DisplayName("Gửi OTP -> xác thực OTP riêng bước đầu -> đổi mật khẩu thành công -> chặn gọi gửi lại trong 5 phút")
    void forgotVerifyAndResetPasswordFlow() {
        User user = new User();
        user.setId(42L);
        user.setEmail("demo@brewlite.vn");
        user.setFullName("Demo User");

        when(userRepository.findByEmail("demo@brewlite.vn")).thenReturn(Optional.of(user));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass123")).thenReturn("hashed-new-pass");

        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("Demo@brewlite.vn");

        ForgotPasswordResponse forgotRes = authService.forgotPassword(forgotReq);
        assertThat(forgotRes.getResetToken()).hasSize(6);
        assertThat(forgotRes.getExpiresInSeconds()).isEqualTo(900L);
        assertThat(forgotRes.getResendAfterSeconds()).isEqualTo(300L);

        assertThatThrownBy(() -> authService.forgotPassword(forgotReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Vui lòng đợi thêm");

        VerifyOtpRequest badVerify = new VerifyOtpRequest();
        badVerify.setEmail("demo@brewlite.vn");
        badVerify.setOtpToken("000000".equals(forgotRes.getResetToken()) ? "111111" : "000000");
        assertThatThrownBy(() -> authService.verifyResetOtp(badVerify))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã OTP không chính xác");

        VerifyOtpRequest goodVerify = new VerifyOtpRequest();
        goodVerify.setEmail("demo@brewlite.vn");
        goodVerify.setOtpToken(forgotRes.getResetToken());
        authService.verifyResetOtp(goodVerify);

        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setEmail("demo@brewlite.vn");
        resetReq.setOtpToken(forgotRes.getResetToken());
        resetReq.setNewPassword("NewPass123");
        authService.resetPassword(resetReq);

        assertThat(user.getPasswordHash()).isEqualTo("hashed-new-pass");
        verify(userRepository).save(any(User.class));

        assertThatThrownBy(() -> authService.verifyResetOtp(goodVerify))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
