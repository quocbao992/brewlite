package vn.doancnpm.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data 
public class ResetPasswordRequest {
    @NotBlank
    private String email;

    @NotBlank 
    private String otpToken;

    @NotBlank
    private String newPassword;
}
