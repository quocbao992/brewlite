package vn.doancnpm.service.implement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.ForgotPasswordRequest;
import vn.doancnpm.dto.Request.LoginRequest;
import vn.doancnpm.dto.Request.RefreshTokenRequest;
import vn.doancnpm.dto.Request.RegisterRequest;
import vn.doancnpm.dto.Request.ResetPasswordRequest;
import vn.doancnpm.dto.Request.VerifyOtpRequest;
import vn.doancnpm.dto.Response.LoginResponse;
import vn.doancnpm.dto.Response.RefreshTokenResponse;
import vn.doancnpm.dto.Response.ForgotPasswordResponse;
import vn.doancnpm.dto.Response.RegisterResponse;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.repository.UserRepository;
import vn.doancnpm.service.AuthService;
import vn.doancnpm.service.JWTUtils;
import vn.doancnpm.repository.CustomerRepository;


@Service
@RequiredArgsConstructor
public class IAuthService implements AuthService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JWTUtils jwtUtils;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.webhook-url:}")
    private String mailWebhookUrl;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    private static final long OTP_TTL_SECONDS = 15 * 60L;
    private static final long OTP_RESEND_COOLDOWN_SECONDS = 5 * 60L;
    private static final String PASSWORD_RESET_KEY_PREFIX = "password-reset:";
    private static final String PASSWORD_RESET_COOLDOWN_KEY_PREFIX = "password-reset-cooldown:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private record InMemoryEntry(String value, long expireAt) {
        boolean isExpired(long now) {
            return now > expireAt;
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, InMemoryEntry> inMemoryStore = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    private void purgeExpiredEntries() {
        long now = System.currentTimeMillis();
        inMemoryStore.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private void storeValue(String key, String value, long timeout, TimeUnit unit) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
                return;
            } catch (Throwable t) {
                redisAvailable = false;
            }
        }
        purgeExpiredEntries();
        long expireAt = System.currentTimeMillis() + unit.toMillis(timeout);
        inMemoryStore.put(key, new InMemoryEntry(value, expireAt));
    }

    private String getValue(String key) {
        if (redisAvailable) {
            try {
                String val = redisTemplate.opsForValue().get(key);
                if (val != null) {
                    return val;
                }
            } catch (Throwable t) {
                redisAvailable = false;
            }
        }
        InMemoryEntry entry = inMemoryStore.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(System.currentTimeMillis())) {
            inMemoryStore.remove(key);
            return null;
        }
        return entry.value();
    }

    private void deleteValue(String key) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (Throwable t) {
                redisAvailable = false;
            }
        }
        inMemoryStore.remove(key);
    }

    private boolean hasKey(String key) {
        if (redisAvailable) {
            try {
                Boolean has = redisTemplate.hasKey(key);
                if (has != null && has) {
                    return true;
                }
            } catch (Throwable t) {
                redisAvailable = false;
            }
        }
        return getValue(key) != null;
    }

    private UserResponse covertToResponse(User user) {
        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
                .customerId(customer != null ? customer.getId() : user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(customer != null ? customer.getPhone() : null)
                .role(user.getRole())
                .status(user.isStatus())
                .dateOfBirth(customer != null ? customer.getDate() : null)
                .gender(customer != null ? customer.isGender() : null)
                .build();
    }

    private RegisterResponse convertToRegisterResponse(User user) {
        return new RegisterResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private String normalizePhone(String phone) {
        String normalizedPhone = phone.trim();

        if (normalizedPhone.startsWith("+84")) {
            normalizedPhone =
                    "0" + normalizedPhone.substring(3);
        }

        return normalizedPhone;
    }

    

    @Override 
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        

        if(!user.isStatus()) {
            throw new  DisabledException("Tài khoản đã bị khóa");
        }

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        String accessToken = jwtUtils.generateToken(user.getUsername());
        String refreshToken = UUID.randomUUID().toString();

        long refreshTokenDurationDays = 7;
        storeValue(
                "refresh:" + refreshToken,
                user.getUsername(),
                refreshTokenDurationDays,
                TimeUnit.DAYS
        );

        UserResponse userResponse = covertToResponse(user);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400000L)
                .user(userResponse)
                .build();



    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()
                && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp");
        }

        String username = request.getUsername().trim();

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        String fullName = request.getFullName().trim();
        String phone = normalizePhone(request.getPhone());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập đã được sử dụng"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email đã được sử dụng"
            );
        }

        if (customerRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException(
                    "Số điện thoại đã được sử dụng"
            );
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("CUSTOMER");
        user.setStatus(true);

        User savedUser = userRepository.save(user);

        Customer customer = Customer.builder().user(savedUser).phone(phone).date(request.getDateOfBirth()).gender(request.getGender()).loyaltyP(0).build();

        customerRepository.save(customer);

        return convertToRegisterResponse(savedUser);
    }

    @Override 
    public void logout(String accesstoken,String refreshtoken) {
        if(accesstoken != null ) {
            long remain = jwtUtils.getRemainingExpiration(accesstoken);
            if(remain > 0 ) {
                storeValue("blacklist:" + accesstoken, "logout", remain, TimeUnit.MILLISECONDS);
            }
        }

        if(refreshtoken != null && !refreshtoken.isBlank()) {
            deleteValue("refresh:" + refreshtoken);
        }
        SecurityContextHolder.clearContext();
    }

    @Override 
    public boolean isTokenBlacklisted(String token) {
        return hasKey("blacklist:" + token) || hasKey("blacklisted:" + token);
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String incomingRefreshToken = request.getRefreshToken();

        String redisKey = "refresh:" + incomingRefreshToken;
        String username = getValue(redisKey);

        if (username == null) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        deleteValue(redisKey);

        String newAccessToken = jwtUtils.generateToken(username);
        String newRefreshToken = UUID.randomUUID().toString();

        storeValue(
            "refresh:" + newRefreshToken,
            username,
            7,
            TimeUnit.DAYS
        );

        return RefreshTokenResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(86400000L)
                .build();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp) throws Exception {
        String subject = "Mã OTP đặt lại mật khẩu - BrewLite Coffee";
        String body = "Xin chào " + fullName + ",\n\n"
                + "Mã OTP đặt lại mật khẩu của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong 15 phút. Vui lòng không chia sẻ mã này cho người khác.";

        if (isPresent(brevoApiKey)) {
            String senderEmail = isPresent(mailUsername) ? mailUsername : "no-reply@brewlite.vn";
            String payload = objectMapper.writeValueAsString(Map.of(
                    "sender", Map.of("name", "BrewLite Coffee", "email", senderEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "textContent", body
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(10))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Brevo status " + response.statusCode());
            }
            return;
        }

        if (isPresent(mailWebhookUrl)) {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "to", toEmail,
                    "subject", subject,
                    "body", body
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(mailWebhookUrl.trim()))
                    .timeout(Duration.ofSeconds(10))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                throw new IllegalStateException("Webhook status " + response.statusCode());
            }
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String formatRemainingTime(long totalSeconds) {
        long safeSeconds = Math.max(0L, totalSeconds);
        long minutes = safeSeconds / 60L;
        long seconds = safeSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private long parseEpochMillis(String rawValue) {
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Long validateResetOtp(String rawEmail, String rawOtp) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String redisKey = PASSWORD_RESET_KEY_PREFIX + email;
        String stored = getValue(redisKey);
        if (stored == null || !stored.contains(":")) {
            throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        String[] parts = stored.split(":", 2);
        String savedOtp = parts[0];
        String userId = parts[1];
        if (!savedOtp.equals(rawOtp.trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
        }
        return Long.valueOf(userId);
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String cooldownKey = PASSWORD_RESET_COOLDOWN_KEY_PREFIX + email;
        String existingCooldown = getValue(cooldownKey);
        if (isPresent(existingCooldown)) {
            long allowedAt = parseEpochMillis(existingCooldown);
            long remainingMs = allowedAt - System.currentTimeMillis();
            if (remainingMs > 0) {
                long remainingSeconds = (remainingMs + 999L) / 1000L;
                throw new IllegalStateException(
                        "Vui lòng đợi thêm " + formatRemainingTime(remainingSeconds) + " trước khi gửi lại mã OTP.");
            }
        }

        long nextResendAt = System.currentTimeMillis() + OTP_RESEND_COOLDOWN_SECONDS * 1000L;
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            storeValue(cooldownKey, String.valueOf(nextResendAt), OTP_RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
            return new ForgotPasswordResponse(
                    "Nếu email tồn tại, hệ thống đã gửi mã OTP đặt lại mật khẩu.",
                    null,
                    OTP_TTL_SECONDS,
                    OTP_RESEND_COOLDOWN_SECONDS
            );
        }

        String resetOtp = String.format("%06d", secureRandom.nextInt(1000000));
        String redisKey = PASSWORD_RESET_KEY_PREFIX + email;
        storeValue(
                redisKey,
                resetOtp + ":" + user.getId(),
                OTP_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        storeValue(
                cooldownKey,
                String.valueOf(nextResendAt),
                OTP_RESEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );

        boolean mailConfigured = isPresent(brevoApiKey)
                || isPresent(mailWebhookUrl)
                || (mailSender != null && isPresent(mailUsername));

        if (mailConfigured) {
            try {
                sendOtpEmail(email, user.getFullName(), resetOtp);
            } catch (Exception ex) {
                deleteValue(redisKey);
                deleteValue(cooldownKey);
                throw new IllegalStateException("Không thể gửi email OTP, vui lòng thử lại sau.");
            }
            return new ForgotPasswordResponse(
                    "Mã OTP 6 số đã được gửi tới email của bạn.",
                    null,
                    OTP_TTL_SECONDS,
                    OTP_RESEND_COOLDOWN_SECONDS
            );
        }

        return new ForgotPasswordResponse(
                "Mã OTP đặt lại mật khẩu đã được tạo.",
                resetOtp,
                OTP_TTL_SECONDS,
                OTP_RESEND_COOLDOWN_SECONDS
        );
    }

    @Override
    public void verifyResetOtp(VerifyOtpRequest request) {
        validateResetOtp(request.getEmail(), request.getOtpToken());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Long userId = validateResetOtp(email, request.getOtpToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        deleteValue(PASSWORD_RESET_KEY_PREFIX + email);
        deleteValue(PASSWORD_RESET_COOLDOWN_KEY_PREFIX + email);
    }
}
