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
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.ForgotPasswordRequest;
import vn.doancnpm.dto.Request.LoginRequest;
import vn.doancnpm.dto.Request.RefreshTokenRequest;
import vn.doancnpm.dto.Request.RegisterRequest;
import vn.doancnpm.dto.Request.ResetPasswordRequest;
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

    private final SecureRandom secureRandom = new SecureRandom();

    private final java.util.concurrent.ConcurrentHashMap<String, String> inMemoryStore = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    private void storeValue(String key, String value, long timeout, TimeUnit unit) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
                return;
            } catch (Throwable t) {
                redisAvailable = false;
            }
        }
        inMemoryStore.put(key, value);
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
        return inMemoryStore.get(key);
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
        return inMemoryStore.containsKey(key);
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

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return new ForgotPasswordResponse(
                    "Nếu email tồn tại, hệ thống đã gửi mã OTP đặt lại mật khẩu.",
                    null
            );
        }

        String resetOtp = String.format("%06d", secureRandom.nextInt(1000000));
        String redisKey = "password-reset:" + email + ":" + resetOtp;
        storeValue(
                redisKey,
                user.getId().toString(),
                15,
                TimeUnit.MINUTES
        );

        boolean mailConfigured = mailSender != null && mailUsername != null && !mailUsername.isBlank();
        if (mailConfigured) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailUsername);
                message.setTo(email);
                message.setSubject("Mã OTP đặt lại mật khẩu");
                message.setText(
                        "Xin chào " + user.getFullName() + ",\n\n"
                        + "Mã OTP đặt lại mật khẩu của bạn là: " + resetOtp + "\n"
                        + "Mã có hiệu lực trong 15 phút. Vui lòng không chia sẽ mã này cho người khác."
                );
                mailSender.send(message);
            } catch (Exception ex) {
                deleteValue(redisKey);
                throw new IllegalStateException("Không thể gửi email OTP, vui lòng thử lại sau.");
            }
            return new ForgotPasswordResponse(
                    "Mã OTP 6 số đã được gửi tới email của bạn (có hiệu lực trong 15 phút).",
                    null
            );
        }

        return new ForgotPasswordResponse(
                "Mã OTP đặt lại mật khẩu có hiệu lực trong 15 phút.",
                resetOtp
        );
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String redisKey = "password-reset:" + email + ":" + request.getOtpToken().trim();
        String userId = getValue(redisKey);
        if (userId == null) {
            throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        deleteValue(redisKey);
    }
}
