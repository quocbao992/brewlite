package vn.doancnpm.service;

import vn.doancnpm.dto.Request.LoginRequest;
import vn.doancnpm.dto.Request.RefreshTokenRequest;
import vn.doancnpm.dto.Request.RegisterRequest;
import vn.doancnpm.dto.Request.ResetPasswordRequest;
import vn.doancnpm.dto.Response.LoginResponse;
import vn.doancnpm.dto.Response.RefreshTokenResponse;
import vn.doancnpm.dto.Request.ForgotPasswordRequest;
import vn.doancnpm.dto.Response.ForgotPasswordResponse;
import vn.doancnpm.dto.Response.RegisterResponse;


public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    
    RegisterResponse register(RegisterRequest registerRequest);

    void logout(String accesstoken,String refreshtoken);

    boolean isTokenBlacklisted(String token);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);
    
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
