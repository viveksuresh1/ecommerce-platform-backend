package com.ecommerce.platform.order.application.service;

import com.ecommerce.platform.cart.domain.model.Cart;
import com.ecommerce.platform.cart.domain.model.CartItem;
import com.ecommerce.platform.cart.domain.repository.CartRepository;
import com.ecommerce.platform.inventory.application.service.InventoryService;
import com.ecommerce.platform.order.api.dto.CreateOrderRequest;
import com.ecommerce.platform.order.api.dto.OrderListResponse;
import com.ecommerce.platform.order.api.dto.OrderResponse;
import com.ecommerce.platform.order.api.dto.UpdateOrderStatusRequest;
import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.model.OrderItem;
import com.ecommerce.platform.order.domain.model.OrderStatus;
import com.ecommerce.platform.order.domain.model.OrderStatusHistory;
import com.ecommerce.platform.order.domain.model.PaymentStatus;
import com.ecommerce.platform.order.domain.repository.OrderRepository;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.ForbiddenException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.domain.model.Address;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.AddressRepository;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for order operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    /**
     * Create order from cart.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = getCurrentUser();

        // Get cart with items
        Cart cart = cartRepository.findByUserIdWithItemsAndProducts(user.getId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Get shipping address
        String shippingName, shippingPhone, shippingStreet, shippingCity, shippingState, shippingPostalCode, shippingCountry;

        if (request.getShippingAddressId() != null) {
            Address address = addressRepository.findById(request.getShippingAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getShippingAddressId()));
            if (!address.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("Address does not belong to user");
            }
            shippingName = user.getFirstName() + " " + user.getLastName();
            shippingPhone = user.getPhone();
            shippingStreet = address.getStreet();
            shippingCity = address.getCity();
            shippingState = address.getState();
            shippingPostalCode = address.getPostalCode();
            shippingCountry = address.getCountry();
        } else if (request.getShippingAddress() != null) {
            CreateOrderRequest.ShippingAddress addr = request.getShippingAddress();
            shippingName = addr.getName();
            shippingPhone = addr.getPhone();
            shippingStreet = addr.getStreet();
            shippingCity = addr.getCity();
            shippingState = addr.getState();
            shippingPostalCode = addr.getPostalCode();
            shippingCountry = addr.getCountry() != null ? addr.getCountry() : "India";
        } else {
            throw new BadRequestException("Shipping address is required");
        }

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .shippingName(shippingName)
                .shippingPhone(shippingPhone)
                .shippingStreet(shippingStreet)
                .shippingCity(shippingCity)
                .shippingState(shippingState)
                .shippingPostalCode(shippingPostalCode)
                .shippingCountry(shippingCountry)
                .notes(request.getNotes())
                .build();

        // Add items from cart
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .productSku(cartItem.getProduct().getSku())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .build();
            orderItem.calculateTotal();
            order.addItem(orderItem);

            // Deduct inventory
            inventoryService.deductStock(
                    cartItem.getProduct().getId(),
                    cartItem.getQuantity(),
                    "ORDER",
                    order.getId()
            );
        }

        order.calculateTotals();

        // Add initial status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.PENDING)
                .notes("Order placed")
                .createdBy(user)
                .build();
        order.addStatusHistory(history);

        order = orderRepository.save(order);

        // Clear cart
        cart.clear();
        cartRepository.save(cart);

        log.info("Order {} created for user {}", order.getOrderNumber(), user.getEmail());
        return toOrderResponse(order);
    }

    /**
     * Get order by ID (user's own order).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return toOrderResponse(order);
    }

    /**
     * Get order by order number.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        User user = getCurrentUser();
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return toOrderResponse(order);
    }

    /**
     * Get user's orders.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderListResponse> getMyOrders(Pageable pageable) {
        User user = getCurrentUser();
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        Page<OrderListResponse> responsePage = orders.map(this::toOrderListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Cancel order (user).
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order cannot be cancelled in current status");
        }

        // Restore inventory
        for (OrderItem item : order.getItems()) {
            inventoryService.addStock(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    "Order " + order.getOrderNumber() + " cancelled"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(OrderStatus.CANCELLED)
                .notes("Cancelled by customer")
                .createdBy(user)
                .build();
        order.addStatusHistory(history);

        order = orderRepository.save(order);
        log.info("Order {} cancelled by user", order.getOrderNumber());

        return toOrderResponse(order);
    }

    // ==================== Admin Methods ====================

    /**
     * Get all orders (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderListResponse> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        Page<OrderListResponse> responsePage = orders.map(this::toOrderListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get orders by status (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderListResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        Page<OrderListResponse> responsePage = orders.map(this::toOrderListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get order by ID (admin - any order).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderAdmin(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return toOrderResponse(order);
    }

    /**
     * Update order status (admin).
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        User admin = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus newStatus = request.getStatus();

        // Update payment status if delivered
        if (newStatus == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        // Restore inventory if cancelling
        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                inventoryService.addStock(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        "Order " + order.getOrderNumber() + " cancelled by admin"
                );
            }
        }

        order.setStatus(newStatus);
        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(newStatus)
                .notes(request.getNotes())
                .createdBy(admin)
                .build();
        order.addStatusHistory(history);

        order = orderRepository.save(order);
        log.info("Order {} status updated to {} by admin", order.getOrderNumber(), newStatus);

        return toOrderResponse(order);
    }

    // ==================== Helper Methods ====================

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + uniquePart;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productSlug(item.getProduct().getSlug())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .shippingAddress(OrderResponse.ShippingAddressResponse.builder()
                        .name(order.getShippingName())
                        .phone(order.getShippingPhone())
                        .street(order.getShippingStreet())
                        .city(order.getShippingCity())
                        .state(order.getShippingState())
                        .postalCode(order.getShippingPostalCode())
                        .country(order.getShippingCountry())
                        .build())
                .subtotal(order.getSubtotal())
                .shippingCost(order.getShippingCost())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderListResponse toOrderListResponse(Order order) {
        return OrderListResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .itemCount(order.getItems().size())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
