package vn.doancnpm.service.implement;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import lombok.RequiredArgsConstructor;
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
@Transactional
public class IAuthService implements AuthService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JWTUtils jwtUtils;

    private UserResponse covertToResponse(User user) {
        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
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
            throw new BadCredentialsException("Tên đăng nhặp hoặc mật khẩu không chính xác");
        }

        String accessToken = jwtUtils.generateToken(user.getUsername());
        String refreshToken = UUID.randomUUID().toString();

        long refreshTokenDurationDays = 7;
        redisTemplate.opsForValue().set(
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
    public RegisterResponse register(RegisterRequest request) {
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
                redisTemplate.opsForValue().set("blacklist:" + accesstoken, "logout", remain, TimeUnit.MILLISECONDS);
            }
        }

        if(refreshtoken != null && !refreshtoken.isBlank()) {
            redisTemplate.delete("refresh:" + refreshtoken);
        }
        SecurityContextHolder.clearContext();
    }

    @Override 
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklisted:"+ token));
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String incomingRefreshToken = request.getRefreshToken();

        String redisKey = "refresh:" + incomingRefreshToken;
        String username = redisTemplate.opsForValue().get(redisKey);

        if (username == null) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        redisTemplate.delete(redisKey);

        String newAccessToken = jwtUtils.generateToken(username);
        String newRefreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
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

        if(user == null) {
            return new ForgotPasswordResponse(
                    "Nếu email tồn tại, hệ thống đã tạo yêu cầu đặt lại mật khẩu.",
                    null
            );
        }

        String resetOtp = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "password-reset:" + resetOtp,
                user.getId().toString(),
                15,
                TimeUnit.MINUTES
        );

        return new ForgotPasswordResponse(
                "Token đặt lại mật khẩu có hiệu lực trong 15 phút.",
                resetOtp
        );
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String redisKey = "password-reset:" + request.getOtpToken();
        String userId = redisTemplate.opsForValue().get(redisKey);
        if(userId == null) {
            throw new IllegalArgumentException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        redisTemplate.delete(redisKey);

    }
}
