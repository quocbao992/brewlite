package vn.doancnpm.controller;

import java.util.Map;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import vn.doancnpm.dto.Request.LoginRequest;
import vn.doancnpm.dto.Request.ForgotPasswordRequest;
import vn.doancnpm.dto.Request.LogoutRequest;
import vn.doancnpm.dto.Request.RefreshTokenRequest;
import vn.doancnpm.dto.Response.LoginResponse;
import vn.doancnpm.dto.Response.ForgotPasswordResponse;
import vn.doancnpm.dto.Response.RefreshTokenResponse;
import vn.doancnpm.dto.Request.RegisterRequest;
import vn.doancnpm.dto.Request.ResetPasswordRequest;
import vn.doancnpm.dto.Response.RegisterResponse;
import vn.doancnpm.service.AuthService;

@Valid 
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor 

public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {
    
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
    
        authService.logout(accessToken, request != null ? request.getRefreshToken() : null);
    
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request ) {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message","Đặt lại mật khẩu thành công"));
    }
}
