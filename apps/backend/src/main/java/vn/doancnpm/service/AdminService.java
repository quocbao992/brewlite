package vn.doancnpm.service;

import vn.doancnpm.dto.Request.UserRequest;
import vn.doancnpm.dto.Response.UserResponse;

import java.util.List;

public interface AdminService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    List<UserResponse> getUsersByRole(String role);
    UserResponse createUser(UserRequest request);
    UserResponse updateUser(Long id, UserRequest request);
    UserResponse updateStatus(Long id, boolean status);
    UserResponse updateRole(Long id, String role);
}
