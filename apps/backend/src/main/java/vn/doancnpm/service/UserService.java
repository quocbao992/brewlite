package vn.doancnpm.service;

import vn.doancnpm.dto.Request.ChangePasswordRequest;
import vn.doancnpm.dto.Request.UpdateProfileRequest;
import vn.doancnpm.dto.Response.UserResponse;

public interface UserService {
    UserResponse getCurrentUserProfile(Long userId);
    UserResponse updateCurrentUserProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}