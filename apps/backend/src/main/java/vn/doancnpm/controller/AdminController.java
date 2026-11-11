package vn.doancnpm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.dto.Request.UserRequest;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.service.AdminService;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(adminService.getUsersByRole(role));
        }
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                            @RequestParam boolean status) {
        UserResponse user = adminService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "status", user.getStatus(),
                "message", "Cập nhật trạng thái thành công"
        ));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id,
                                                          @RequestParam String role) {
        UserResponse user = adminService.updateRole(id, role);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }
}
