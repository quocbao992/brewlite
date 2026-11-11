package vn.doancnpm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import vn.doancnpm.config.GlobalExceptionHandler;
import vn.doancnpm.controller.AdminController;
import vn.doancnpm.dto.Request.UserRequest;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/users";

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private UserResponse createUserResponse(Long id, String username, String email, String role, Boolean status) {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .fullName("Nguyen Van A")
                .email(email)
                .phone("0901234567")
                .role(role)
                .status(status)
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .gender(true)
                .build();
    }

    private UserRequest createValidUserRequest() {
        UserRequest request = new UserRequest();
        request.setUsername("testadmin");
        request.setFullName("Nguyen Van Admin");
        request.setEmail("admin@brewlite.vn");
        request.setPassword("StrongPass123@");
        request.setRole("ADMIN");
        request.setPhone("0901234567");
        return request;
    }

    // =========================================================================
    // 1. GET /api/v1/admin/users
    // =========================================================================
    @Nested
    @DisplayName("GET " + BASE_URL)
    class GetAllUsersTests {

        @Test
        @DisplayName("Không truyền param -> lấy tất cả người dùng thành công")
        void getAllUsers_withoutRoleParam_returnsAllUsers() throws Exception {
            List<UserResponse> users = List.of(
                    createUserResponse(1L, "admin", "admin@brewlite.vn", "ADMIN", true),
                    createUserResponse(2L, "cashier", "cashier@brewlite.vn", "CASHIER", true)
            );
            when(adminService.getAllUsers()).thenReturn(users);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].username").value("admin"))
                    .andExpect(jsonPath("$[0].role").value("ADMIN"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].username").value("cashier"))
                    .andExpect(jsonPath("$[1].role").value("CASHIER"));

            verify(adminService).getAllUsers();
            verify(adminService, never()).getUsersByRole(any());
        }

        @Test
        @DisplayName("Role là chuỗi rỗng hoặc khoảng trắng -> lấy tất cả người dùng")
        void getAllUsers_withBlankRole_returnsAllUsers() throws Exception {
            List<UserResponse> users = List.of(
                    createUserResponse(1L, "admin", "admin@brewlite.vn", "ADMIN", true)
            );
            when(adminService.getAllUsers()).thenReturn(users);

            mockMvc.perform(get(BASE_URL).param("role", "   "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].username").value("admin"));

            verify(adminService).getAllUsers();
            verify(adminService, never()).getUsersByRole(any());
        }

        @Test
        @DisplayName("Có truyền param role -> lọc danh sách theo role")
        void getAllUsers_withRoleParam_returnsFilteredUsers() throws Exception {
            List<UserResponse> admins = List.of(
                    createUserResponse(1L, "admin", "admin@brewlite.vn", "ADMIN", true)
            );
            when(adminService.getUsersByRole("ADMIN")).thenReturn(admins);

            mockMvc.perform(get(BASE_URL).param("role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].username").value("admin"))
                    .andExpect(jsonPath("$[0].role").value("ADMIN"));

            verify(adminService).getUsersByRole("ADMIN");
            verify(adminService, never()).getAllUsers();
        }

        @Test
        @DisplayName("Danh sách trống -> trả về mảng rỗng")
        void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
            when(adminService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(adminService).getAllUsers();
        }
    }

    // =========================================================================
    // 2. GET /api/v1/admin/users/{id}
    // =========================================================================
    @Nested
    @DisplayName("GET " + BASE_URL + "/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("User tồn tại -> trả về 200 và chi tiết người dùng")
        void getUserById_existingUser_returnsUser() throws Exception {
            UserResponse response = createUserResponse(1L, "admin", "admin@brewlite.vn", "ADMIN", true);
            when(adminService.getUserById(1L)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.email").value("admin@brewlite.vn"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.status").value(true));

            verify(adminService).getUserById(1L);
        }

        @Test
        @DisplayName("User không tồn tại -> trả về 404 Not Found")
        void getUserById_notFound_returns404() throws Exception {
            when(adminService.getUserById(999L))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"));

            mockMvc.perform(get(BASE_URL + "/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(adminService).getUserById(999L);
        }
    }

    // =========================================================================
    // 3. POST /api/v1/admin/users
    // =========================================================================
    @Nested
    @DisplayName("POST " + BASE_URL)
    class CreateUserTests {

        @Test
        @DisplayName("Dữ liệu hợp lệ -> tạo người dùng thành công trả về 200")
        void createUser_validRequest_returnsCreatedUser() throws Exception {
            UserRequest request = createValidUserRequest();
            UserResponse response = createUserResponse(10L, request.getUsername(), request.getEmail(), request.getRole(), true);
            when(adminService.createUser(any(UserRequest.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.username").value("testadmin"))
                    .andExpect(jsonPath("$.email").value("admin@brewlite.vn"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            ArgumentCaptor<UserRequest> captor = ArgumentCaptor.forClass(UserRequest.class);
            verify(adminService).createUser(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("testadmin");
        }

        @Test
        @DisplayName("Validation lỗi: fullName rỗng -> trả về 400")
        void createUser_blankFullName_returns400() throws Exception {
            UserRequest request = createValidUserRequest();
            request.setFullName("");

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("Validation lỗi: email không hợp lệ -> trả về 400")
        void createUser_invalidEmail_returns400() throws Exception {
            UserRequest request = createValidUserRequest();
            request.setEmail("not-an-email");

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("Validation lỗi: role không thuộc danh sách hợp lệ -> trả về 400")
        void createUser_invalidRole_returns400() throws Exception {
            UserRequest request = createValidUserRequest();
            request.setRole("SUPER_ADMIN");

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("Service ném IllegalArgumentException (username đã tồn tại) -> trả về 400")
        void createUser_usernameExists_returns400() throws Exception {
            UserRequest request = createValidUserRequest();
            when(adminService.createUser(any(UserRequest.class)))
                    .thenThrow(new IllegalArgumentException("Tên đăng nhập đã tồn tại"));

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Tên đăng nhập đã tồn tại"));

            verify(adminService).createUser(any(UserRequest.class));
        }
    }

    // =========================================================================
    // 4. PUT /api/v1/admin/users/{id}
    // =========================================================================
    @Nested
    @DisplayName("PUT " + BASE_URL + "/{id}")
    class UpdateUserTests {

        @Test
        @DisplayName("Cập nhật thành công -> trả về 200 và thông tin đã cập nhật")
        void updateUser_validRequest_returnsUpdatedUser() throws Exception {
            UserRequest request = createValidUserRequest();
            UserResponse response = createUserResponse(1L, "testadmin", "updated@brewlite.vn", "ADMIN", true);
            when(adminService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("updated@brewlite.vn"));

            verify(adminService).updateUser(eq(1L), any(UserRequest.class));
        }

        @Test
        @DisplayName("Validation lỗi khi cập nhật -> trả về 400")
        void updateUser_invalidRequest_returns400() throws Exception {
            UserRequest request = createValidUserRequest();
            request.setEmail("bad-email");

            mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("User không tồn tại khi cập nhật -> trả về 404")
        void updateUser_userNotFound_returns404() throws Exception {
            UserRequest request = createValidUserRequest();
            when(adminService.updateUser(eq(999L), any(UserRequest.class)))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"));

            mockMvc.perform(put(BASE_URL + "/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(adminService).updateUser(eq(999L), any(UserRequest.class));
        }
    }

    // =========================================================================
    // 5. PATCH /api/v1/admin/users/{id}/status
    // =========================================================================
    @Nested
    @DisplayName("PATCH " + BASE_URL + "/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("Cập nhật trạng thái thành công -> trả về 200 và thông báo thành công")
        void updateStatus_success_returnsSuccessMessage() throws Exception {
            UserResponse response = createUserResponse(5L, "staff", "staff@brewlite.vn", "CASHIER", false);
            when(adminService.updateStatus(5L, false)).thenReturn(response);

            mockMvc.perform(patch(BASE_URL + "/{id}/status", 5L)
                    .param("status", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.username").value("staff"))
                    .andExpect(jsonPath("$.status").value(false))
                    .andExpect(jsonPath("$.message").value("Cập nhật trạng thái thành công"));

            verify(adminService).updateStatus(5L, false);
        }

        @Test
        @DisplayName("Thiếu param status -> trả về 400 Bad Request")
        void updateStatus_missingParam_returns400() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{id}/status", 5L))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("User không tồn tại khi đổi trạng thái -> trả về 404")
        void updateStatus_userNotFound_returns404() throws Exception {
            when(adminService.updateStatus(999L, true))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"));

            mockMvc.perform(patch(BASE_URL + "/{id}/status", 999L)
                    .param("status", "true"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(adminService).updateStatus(999L, true);
        }
    }

    // =========================================================================
    // 6. PATCH /api/v1/admin/users/{id}/role
    // =========================================================================
    @Nested
    @DisplayName("PATCH " + BASE_URL + "/{id}/role")
    class UpdateRoleTests {

        @Test
        @DisplayName("Cập nhật vai trò thành công -> trả về 200 và thông tin role mới")
        void updateRole_success_returnsUpdatedRole() throws Exception {
            UserResponse response = createUserResponse(5L, "staff", "staff@brewlite.vn", "BARISTA", true);
            when(adminService.updateRole(5L, "BARISTA")).thenReturn(response);

            mockMvc.perform(patch(BASE_URL + "/{id}/role", 5L)
                    .param("role", "BARISTA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.username").value("staff"))
                    .andExpect(jsonPath("$.role").value("BARISTA"));

            verify(adminService).updateRole(5L, "BARISTA");
        }

        @Test
        @DisplayName("Thiếu param role -> trả về 400 Bad Request")
        void updateRole_missingParam_returns400() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{id}/role", 5L))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(adminService);
        }

        @Test
        @DisplayName("User không tồn tại khi đổi role -> trả về 404")
        void updateRole_userNotFound_returns404() throws Exception {
            when(adminService.updateRole(999L, "ADMIN"))
                    .thenThrow(new EntityNotFoundException("Không tìm thấy người dùng với ID 999"));

            mockMvc.perform(patch(BASE_URL + "/{id}/role", 999L)
                    .param("role", "ADMIN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy người dùng với ID 999"));

            verify(adminService).updateRole(999L, "ADMIN");
        }
    }
}

