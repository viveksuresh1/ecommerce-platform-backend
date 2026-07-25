package com.ecommerce.platform.admin.application.service;

import com.ecommerce.platform.admin.api.dto.*;
import com.ecommerce.platform.inventory.domain.repository.InventoryRepository;
import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.model.OrderStatus;
import com.ecommerce.platform.order.domain.repository.OrderRepository;
import com.ecommerce.platform.product.domain.repository.ProductRepository;
import com.ecommerce.platform.review.domain.repository.ReviewRepository;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.domain.model.Role;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.model.UserStatus;
import com.ecommerce.platform.user.domain.repository.RoleRepository;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusWeeks(1).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().minusMonths(1).atStartOfDay();

        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();

        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.REFUNDED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED)
                .count();

        long lowStockProducts = inventoryRepository.findLowStockItems(Pageable.unpaged()).getTotalElements();
        long pendingReviews = reviewRepository.findByIsApprovedFalseOrderByCreatedAtDesc(Pageable.unpaged()).getTotalElements();

        OrderStatsResponse orderStats = buildOrderStats(allOrders, todayStart, weekStart, monthStart);
        RevenueStatsResponse revenueStats = buildRevenueStats(allOrders, todayStart, weekStart, monthStart);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders)
                .lowStockProducts(lowStockProducts)
                .pendingReviews(pendingReviews)
                .orderStats(orderStats)
                .revenueStats(revenueStats)
                .build();
    }

    private OrderStatsResponse buildOrderStats(List<Order> allOrders, LocalDateTime todayStart,
                                               LocalDateTime weekStart, LocalDateTime monthStart) {
        long todayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(todayStart))
                .count();

        long weekOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(weekStart))
                .count();

        long monthOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(monthStart))
                .count();

        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = allOrders.stream()
                    .filter(o -> o.getStatus() == status)
                    .count();
            ordersByStatus.put(status.name(), count);
        }

        return OrderStatsResponse.builder()
                .todayOrders(todayOrders)
                .weekOrders(weekOrders)
                .monthOrders(monthOrders)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    private RevenueStatsResponse buildRevenueStats(List<Order> allOrders, LocalDateTime todayStart,
                                                   LocalDateTime weekStart, LocalDateTime monthStart) {
        List<Order> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.REFUNDED)
                .toList();

        BigDecimal todayRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(todayStart))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weekRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(weekStart))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthRevenue = completedOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(monthStart))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOrderValue = BigDecimal.ZERO;
        if (!completedOrders.isEmpty()) {
            BigDecimal totalValue = completedOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgOrderValue = totalValue.divide(BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP);
        }

        return RevenueStatsResponse.builder()
                .todayRevenue(todayRevenue)
                .weekRevenue(weekRevenue)
                .monthRevenue(monthRevenue)
                .averageOrderValue(avgOrderValue)
                .build();
    }

    // ==================== User Management ====================

    @Transactional(readOnly = true)
    public PagedResponse<UserManagementResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        Page<UserManagementResponse> responsePage = users.map(this::toUserManagementResponse);
        return PagedResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserManagementResponse> getUsersByStatus(UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findByStatus(status, pageable);
        Page<UserManagementResponse> responsePage = users.map(this::toUserManagementResponse);
        return PagedResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserManagementResponse> searchUsers(String query, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(UserStatus.ACTIVE, query, pageable);
        Page<UserManagementResponse> responsePage = users.map(this::toUserManagementResponse);
        return PagedResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setStatus(request.getStatus());
        user = userRepository.save(user);
        log.info("User {} status updated to {}", userId, request.getStatus());

        return toUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse addUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRoleName()));

        if (user.getRoles().contains(role)) {
            throw new BadRequestException("User already has role: " + request.getRoleName());
        }

        user.addRole(role);
        user = userRepository.save(user);
        log.info("Role {} added to user {}", request.getRoleName(), userId);

        return toUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse removeUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        if (!user.getRoles().contains(role)) {
            throw new BadRequestException("User does not have role: " + roleName);
        }

        if (user.getRoles().size() == 1) {
            throw new BadRequestException("User must have at least one role");
        }

        user.removeRole(role);
        user = userRepository.save(user);
        log.info("Role {} removed from user {}", roleName, userId);

        return toUserManagementResponse(user);
    }

    private UserManagementResponse toUserManagementResponse(User user) {
        long totalOrders = orderRepository.countByUserId(user.getId());

        return UserManagementResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .totalOrders(totalOrders)
                .build();
    }
}
