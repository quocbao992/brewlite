package vn.doancnpm.service.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;



import vn.doancnpm.domain.Customer;
import vn.doancnpm.domain.User;
import vn.doancnpm.dto.Request.CustomerRequest;
import vn.doancnpm.dto.Response.CustomerResponse;
import vn.doancnpm.repository.CustomerRepository;
import vn.doancnpm.repository.UserRepository;
import vn.doancnpm.service.CustomerService;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ICustomerService implements CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    private CustomerResponse convertToResponse(Customer customer) {
        User user = customer.getUser();

        return new CustomerResponse(customer.getId(), user.getUsername(), user.getEmail(), user.getFullName(), customer.getPhone(), customer.getDate(), customer.isGender(), (int) customer.getLoyaltyP());
    }


    @Override 
    public CustomerResponse getMyProfile(Long userId) {
        Customer customer = customerRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy"));
        return convertToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateMyProfile(Long userid, CustomerRequest customerRequest) {
        Customer customer = customerRepository.findByUserId(userid).orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với user ID: " + userid));

        boolean isphone = customerRepository.existsByPhoneAndIdNot(customerRequest.getPhone(), customer.getId());

        if(isphone) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }

        User user = customer.getUser();
        if (user != null) {
            user.setFullName(customerRequest.getFullName());
            userRepository.save(user);
        }

        customer.setPhone(customerRequest.getPhone());
        customer.setDate(customerRequest.getDateOfBirth());
        customer.setGender(customerRequest.isGender());
        customer = customerRepository.save(customer);

        return convertToResponse(customer);
    }
    
    @Override
    public float getMyLoyalPoint(Long userId) {
        Customer customer = customerRepository.findById(userId).orElse(null);
        
        if(customer == null) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng");
        }

        return customer.getLoyaltyP();


    }
    
}
