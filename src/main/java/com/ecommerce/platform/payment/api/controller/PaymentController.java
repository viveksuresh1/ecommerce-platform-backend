package com.ecommerce.platform.payment.api.controller;

import com.ecommerce.platform.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.platform.payment.api.dto.PaymentResponse;
import com.ecommerce.platform.payment.api.dto.ProcessPaymentRequest;
import com.ecommerce.platform.payment.api.dto.RefundRequest;
import com.ecommerce.platform.payment.application.service.PaymentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment endpoints.
 * User: Initiate, view own payments
 * Admin: View all, process COD, refund
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment processing")
public class PaymentController {

    private final PaymentService paymentService;

    // ==================== User Endpoints ====================

    @PostMapping("/payments/initiate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate payment", description = "Start payment for an order")
    public ApiResponse<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        PaymentResponse payment = paymentService.initiatePayment(request);
        return ApiResponse.success(payment, "Payment initiated");
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get payment", description = "Get payment details")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        PaymentResponse payment = paymentService.getPayment(paymentId);
        return ApiResponse.success(payment);
    }

    @GetMapping("/payments/order/{orderId}")
    @Operation(summary = "Get payment for order", description = "Get payment by order ID")
    public ApiResponse<PaymentResponse> getPaymentForOrder(@PathVariable Long orderId) {
        PaymentResponse payment = paymentService.getPaymentForOrder(orderId);
        return ApiResponse.success(payment);
    }

    @GetMapping("/payments")
    @Operation(summary = "Get my payments", description = "Get current user's payments")
    public ApiResponse<PagedResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<PaymentResponse> payments = paymentService.getMyPayments(pageable);
        return ApiResponse.success(payments);
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments", description = "Admin only - all payments")
    public ApiResponse<PagedResponse<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<PaymentResponse> payments = paymentService.getAllPayments(pageable);
        return ApiResponse.success(payments);
    }

    @GetMapping("/admin/payments/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payment (admin)", description = "Admin only - any payment")
    public ApiResponse<PaymentResponse> getPaymentAdmin(@PathVariable Long paymentId) {
        PaymentResponse payment = paymentService.getPaymentAdmin(paymentId);
        return ApiResponse.success(payment);
    }

    @PostMapping("/admin/payments/{paymentId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process payment", description = "Admin only - simulate gateway callback")
    public ApiResponse<PaymentResponse> processPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse payment = paymentService.processPayment(paymentId, request);
        return ApiResponse.success(payment, request.isSuccess() ? "Payment successful" : "Payment failed");
    }

    @PostMapping("/admin/payments/cod/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process COD payment", description = "Admin only - mark COD as paid")
    public ApiResponse<PaymentResponse> processCodPayment(@PathVariable Long orderId) {
        PaymentResponse payment = paymentService.processCodPayment(orderId);
        return ApiResponse.success(payment, "COD payment collected");
    }

    @PostMapping("/admin/payments/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund payment", description = "Admin only - process refund")
    public ApiResponse<PaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request) {
        PaymentResponse payment = paymentService.refundPayment(paymentId, request);
        return ApiResponse.success(payment, "Refund processed");
    }
}
