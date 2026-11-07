package vn.doancnpm.service.implement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.UserRequest;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.UserRepository;
import vn.doancnpm.service.AdminService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IAdminService implements AdminService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    private User getUserEntityById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new EntityNotFoundException(
                    "Không tìm thấy người dùng với ID: " + id);
        }

        return optionalUser.get();
    }

    private void checkDuplicateUserForCreate(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
    }

    private String getNormalizedEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String getValidRole(String role) {
        String roleInUpperCase = role.trim().toUpperCase(Locale.ROOT);

        boolean validRole = roleInUpperCase.equals("CUSTOMER")
                || roleInUpperCase.equals("CASHIER")
                || roleInUpperCase.equals("ADMIN") || roleInUpperCase.equals("BARISTA");

        if (!validRole) {
            throw new IllegalArgumentException("Vai trò người dùng không hợp lệ");
        }

        return roleInUpperCase;
    }

    private UserResponse toUserResponse(User user) {
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



    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = new ArrayList<>();

        for (User user : users) {
            userResponses.add(toUserResponse(user));
        }

        return userResponses;
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = getUserEntityById(id);
        return toUserResponse(user);
    }

    @Override
    public List<UserResponse> getUsersByRole(String role) {
        String validRole = getValidRole(role);
        List<User> users = userRepository.findByRole(validRole);
        List<UserResponse> userResponses = new ArrayList<>();

        for (User user : users) {
            userResponses.add(toUserResponse(user));
        }

        return userResponses;
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        String username = request.getUsername().trim();
        String email = getNormalizedEmail(request.getEmail());

        checkDuplicateUserForCreate(username, email);

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (customerRepository.existsByPhone(request.getPhone().trim())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(getValidRole(request.getRole()));
        user.setStatus(true);

        User savedUser = userRepository.save(user);

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            Customer customer = Customer.builder()
                    .user(savedUser)
                    .phone(request.getPhone().trim())
                    .date(request.getDateOfBirth())
                    .gender(request.getGender() != null ? request.getGender() : false)
                    .loyaltyP(0)
                    .build();
            customerRepository.save(customer);
        }

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = getUserEntityById(id);
        String email = getNormalizedEmail(request.getEmail());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setRole(getValidRole(request.getRole()));

        User savedUser = userRepository.save(user);

        Customer customer = customerRepository.findByUserId(id).orElse(null);
        if (customer != null) {
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                if (customerRepository.existsByPhoneAndIdNot(request.getPhone().trim(), customer.getId())) {
                    throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
                }
                customer.setPhone(request.getPhone().trim());
            }
            if (request.getDateOfBirth() != null) {
                customer.setDate(request.getDateOfBirth());
            }
            if (request.getGender() != null) {
                customer.setGender(request.getGender());
            }
            customerRepository.save(customer);
        } else if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (customerRepository.existsByPhone(request.getPhone().trim())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
            Customer newCustomer = Customer.builder()
                    .user(savedUser)
                    .phone(request.getPhone().trim())
                    .date(request.getDateOfBirth())
                    .gender(request.getGender() != null ? request.getGender() : false)
                    .loyaltyP(0)
                    .build();
            customerRepository.save(newCustomer);
        }

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, boolean status) {
        User user = getUserEntityById(id);
        user.setStatus(status);

        User savedUser = userRepository.save(user);
        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long id, String role) {
        User user = getUserEntityById(id);
        user.setRole(getValidRole(role));

        User savedUser = userRepository.save(user);
        return toUserResponse(savedUser);
    }
}
