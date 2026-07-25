package com.ecommerce.platform.order.api.controller;

import com.ecommerce.platform.order.api.dto.CreateOrderRequest;
import com.ecommerce.platform.order.api.dto.OrderListResponse;
import com.ecommerce.platform.order.api.dto.OrderResponse;
import com.ecommerce.platform.order.api.dto.UpdateOrderStatusRequest;
import com.ecommerce.platform.order.application.service.OrderService;
import com.ecommerce.platform.order.domain.model.OrderStatus;
import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order endpoints.
 * User: Create, view own orders, cancel
 * Admin: View all, update status
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    // ==================== User Endpoints ====================

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create order", description = "Create order from cart")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ApiResponse.success(order, "Order placed successfully");
    }

    @GetMapping("/orders")
    @Operation(summary = "Get my orders", description = "Get current user's orders")
    public ApiResponse<PagedResponse<OrderListResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<OrderListResponse> orders = orderService.getMyOrders(pageable);
        return ApiResponse.success(orders);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order", description = "Get order details")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        OrderResponse order = orderService.getOrder(orderId);
        return ApiResponse.success(order);
    }

    @GetMapping("/orders/number/{orderNumber}")
    @Operation(summary = "Get order by number", description = "Get order by order number")
    public ApiResponse<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse order = orderService.getOrderByNumber(orderNumber);
        return ApiResponse.success(order);
    }

    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel pending/confirmed order")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        OrderResponse order = orderService.cancelOrder(orderId);
        return ApiResponse.success(order, "Order cancelled");
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders", description = "Admin only - all orders")
    public ApiResponse<PagedResponse<OrderListResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<OrderListResponse> orders;
        if (status != null) {
            orders = orderService.getOrdersByStatus(status, pageable);
        } else {
            orders = orderService.getAllOrders(pageable);
        }
        return ApiResponse.success(orders);
    }

    @GetMapping("/admin/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get order (admin)", description = "Admin only - any order")
    public ApiResponse<OrderResponse> getOrderAdmin(@PathVariable Long orderId) {
        OrderResponse order = orderService.getOrderAdmin(orderId);
        return ApiResponse.success(order);
    }

    @PutMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status", description = "Admin only")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(orderId, request);
        return ApiResponse.success(order, "Order status updated");
    }
}
