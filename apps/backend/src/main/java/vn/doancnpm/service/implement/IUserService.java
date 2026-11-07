package vn.doancnpm.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.ChangePasswordRequest;
import vn.doancnpm.dto.Request.UpdateProfileRequest;
import vn.doancnpm.dto.Response.UserResponse;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.UserRepository;
import vn.doancnpm.service.UserService;

@Service
@RequiredArgsConstructor
public class IUserService implements UserService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponse convertToResponse(User user) {
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
    public UserResponse getCurrentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        return convertToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        user.setFullName(request.getFullName().trim());
        userRepository.save(user);

        Customer customer = customerRepository.findByUserId(userId).orElse(null);
        if (customer != null) {
            if (request.getPhone() != null && customerRepository.existsByPhoneAndIdNot(request.getPhone(), customer.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
            customer.setPhone(request.getPhone());
            customer.setDate(request.getDateOfBirth());
            if (request.getGender() != null) {
                customer.setGender(request.getGender());
            }
            customerRepository.save(customer);
        } else if (request.getPhone() != null) {
            if (customerRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
            Customer newCustomer = Customer.builder()
                    .user(user)
                    .phone(request.getPhone())
                    .date(request.getDateOfBirth())
                    .gender(request.getGender() != null ? request.getGender() : false)
                    .loyaltyP(0)
                    .build();
            customerRepository.save(newCustomer);
        }

        return convertToResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
