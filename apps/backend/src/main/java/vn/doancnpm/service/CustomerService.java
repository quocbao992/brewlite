package vn.doancnpm.service;

import vn.doancnpm.dto.Request.CustomerRequest;
import vn.doancnpm.dto.Response.CustomerResponse;

public interface CustomerService {

    CustomerResponse getMyProfile(Long userId);

    CustomerResponse updateMyProfile(Long userId,CustomerRequest request);

    float getMyLoyalPoint(Long userId);

}