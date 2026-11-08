package vn.doancnpm.service;

import java.time.LocalDate;
import java.util.List;

import vn.doancnpm.domain.Order;
import vn.doancnpm.dto.Response.AdminOrderResponse;
import vn.doancnpm.dto.Response.OrderResponse;
import vn.doancnpm.dto.Response.OrderSummaryResponse;
import vn.doancnpm.dto.Response.PageResponse;

public interface OrderService {

    PageResponse<OrderSummaryResponse> getHistory(Long customerId, Order.Status status, int page, int size);

    OrderResponse getById(Long orderId);

    OrderResponse getByCode(String orderCode);

    List<OrderResponse> getQueue(List<Order.Status> statuses);

    OrderResponse updateStatus(Long orderId, Order.Status newStatus, String reason);

    OrderResponse cancel(Long orderId, String reason);

    PageResponse<AdminOrderResponse> adminSearch(LocalDate fromDate, LocalDate toDate,
            Order.Status status, int page, int size);
}