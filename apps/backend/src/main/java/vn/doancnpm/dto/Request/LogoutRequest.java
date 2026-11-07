package vn.doancnpm.dto.Request;

import lombok.Data;

@Data 
public class LogoutRequest {
    private String refreshToken;
}
