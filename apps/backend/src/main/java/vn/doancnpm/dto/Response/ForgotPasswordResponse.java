package vn.doancnpm.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponse {
    private String message;
    private String resetToken;
    private Long expiresInSeconds;
    private Long resendAfterSeconds;

    public ForgotPasswordResponse(String message, String resetToken) {
        this(message, resetToken, null, null);
    }
}
