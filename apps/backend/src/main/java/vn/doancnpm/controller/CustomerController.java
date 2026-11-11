package vn.doancnpm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.doancnpm.dto.Request.CustomerRequest;
import vn.doancnpm.dto.Response.CustomerResponse;
import vn.doancnpm.service.CustomerService;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor 
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/profile")
    public ResponseEntity<CustomerResponse> getMyProfile(@RequestParam Long userId) {
        return ResponseEntity.ok(customerService.getMyProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<CustomerResponse> updateMyProfile(@RequestParam Long userId, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateMyProfile(userId, request));
    }

    @GetMapping("/loyalty-history")
    public ResponseEntity<?> getMyLoyalPoint(@RequestParam Long userId) {
        return ResponseEntity.ok(customerService.getMyLoyalPoint(userId));
    }
}
