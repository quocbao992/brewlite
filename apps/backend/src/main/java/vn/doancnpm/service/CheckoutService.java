package vn.doancnpm.service;

import vn.doancnpm.dto.Request.CheckoutRequest;
import vn.doancnpm.dto.Response.OrderResponse;

public interface CheckoutService {
   OrderResponse checkout(CheckoutRequest rq);  
    OrderResponse confirmOrder(Long orderId);    
}
