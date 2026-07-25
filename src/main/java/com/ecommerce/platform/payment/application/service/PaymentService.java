package com.ecommerce.platform.payment.application.service;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.model.OrderStatus;
import com.ecommerce.platform.order.domain.model.PaymentStatus;
import com.ecommerce.platform.order.domain.repository.OrderRepository;
import com.ecommerce.platform.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.platform.payment.api.dto.PaymentResponse;
import com.ecommerce.platform.payment.api.dto.ProcessPaymentRequest;
import com.ecommerce.platform.payment.api.dto.RefundRequest;
import com.ecommerce.platform.payment.domain.model.Payment;
import com.ecommerce.platform.payment.domain.model.PaymentTransaction;
import com.ecommerce.platform.payment.domain.model.PaymentTransactionStatus;
import com.ecommerce.platform.payment.domain.model.TransactionType;
import com.ecommerce.platform.payment.domain.repository.PaymentRepository;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.ForbiddenException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for payment operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Initiate payment for an order.
     */
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }

        // Check for existing pending payment
        Payment existingPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getStatus() == PaymentTransactionStatus.PENDING) {
            throw new BadRequestException("Payment already initiated for this order");
        }

        // Create payment record
        Payment payment = Payment.builder()
                .order(order)
                .paymentNumber(generatePaymentNumber())
                .amount(order.getTotalAmount())
                .currency("INR")
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .gatewayName(request.getGatewayName())
                .gatewayTransactionId(generateGatewayTransactionId())
                .build();

        // Add initiate transaction
        PaymentTransaction initTxn = PaymentTransaction.builder()
                .transactionType(TransactionType.INITIATE)
                .status(PaymentTransactionStatus.SUCCESS)
                .amount(payment.getAmount())
                .build();
        payment.addTransaction(initTxn);

        payment = paymentRepository.save(payment);

        log.info("Payment {} initiated for order {}", payment.getPaymentNumber(), order.getOrderNumber());
        return toPaymentResponse(payment);
    }

    /**
     * Process payment (simulated gateway callback).
     * In production, this would be called by the payment gateway webhook.
     */
    @Transactional
    public PaymentResponse processPayment(Long paymentId, ProcessPaymentRequest request) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new BadRequestException("Payment is not in pending state");
        }

        payment.setGatewayResponse(request.getGatewayResponse());

        if (request.isSuccess()) {
            payment.setStatus(PaymentTransactionStatus.SUCCESS);

            // Add capture transaction
            PaymentTransaction captureTxn = PaymentTransaction.builder()
                    .transactionType(TransactionType.CAPTURE)
                    .status(PaymentTransactionStatus.SUCCESS)
                    .amount(payment.getAmount())
                    .gatewayResponse(request.getGatewayResponse())
                    .build();
            payment.addTransaction(captureTxn);

            // Update order
            Order order = payment.getOrder();
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            orderRepository.save(order);

            log.info("Payment {} successful for order {}", payment.getPaymentNumber(), order.getOrderNumber());
        } else {
            payment.setStatus(PaymentTransactionStatus.FAILED);
            payment.setFailureReason(request.getFailureReason());
            payment.setRetryCount(payment.getRetryCount() + 1);

            // Add failed transaction
            PaymentTransaction failedTxn = PaymentTransaction.builder()
                    .transactionType(TransactionType.CAPTURE)
                    .status(PaymentTransactionStatus.FAILED)
                    .amount(payment.getAmount())
                    .gatewayResponse(request.getGatewayResponse())
                    .build();
            payment.addTransaction(failedTxn);

            // Update order payment status
            Order order = payment.getOrder();
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            log.info("Payment {} failed for order {}: {}",
                    payment.getPaymentNumber(), order.getOrderNumber(), request.getFailureReason());
        }

        payment = paymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    /**
     * Process COD payment (mark as paid on delivery).
     */
    @Transactional
    public PaymentResponse processCodPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new BadRequestException("Order is not COD");
        }

        // Create or get payment
        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(() -> {
            Payment newPayment = Payment.builder()
                    .order(order)
                    .paymentNumber(generatePaymentNumber())
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .paymentMethod("COD")
                    .build();
            return paymentRepository.save(newPayment);
        });

        payment.setStatus(PaymentTransactionStatus.SUCCESS);

        PaymentTransaction txn = PaymentTransaction.builder()
                .transactionType(TransactionType.CAPTURE)
                .status(PaymentTransactionStatus.SUCCESS)
                .amount(payment.getAmount())
                .gatewayResponse("Cash collected on delivery")
                .build();
        payment.addTransaction(txn);

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        payment = paymentRepository.save(payment);
        log.info("COD payment {} collected for order {}", payment.getPaymentNumber(), order.getOrderNumber());

        return toPaymentResponse(payment);
    }

    /**
     * Refund payment (admin).
     */
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!payment.canBeRefunded()) {
            throw new BadRequestException("Payment cannot be refunded");
        }

        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : payment.getAmount();
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException("Refund amount exceeds payment amount");
        }

        payment.setRefundAmount(refundAmount);
        payment.setRefundReason(request.getReason());
        payment.setRefundedAt(LocalDateTime.now());
        payment.setStatus(PaymentTransactionStatus.REFUNDED);

        PaymentTransaction refundTxn = PaymentTransaction.builder()
                .transactionType(TransactionType.REFUND)
                .status(PaymentTransactionStatus.SUCCESS)
                .amount(refundAmount)
                .gatewayResponse("Refund processed: " + request.getReason())
                .build();
        payment.addTransaction(refundTxn);

        // Update order
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        payment = paymentRepository.save(payment);
        log.info("Payment {} refunded: {} INR", payment.getPaymentNumber(), refundAmount);

        return toPaymentResponse(payment);
    }

    /**
     * Get payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        User user = getCurrentUser();
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return toPaymentResponse(payment);
    }

    /**
     * Get payment for order.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentForOrder(Long orderId) {
        User user = getCurrentUser();
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }

        return toPaymentResponse(payment);
    }

    /**
     * Get user's payments.
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getMyPayments(Pageable pageable) {
        User user = getCurrentUser();
        Page<Payment> payments = paymentRepository.findByUserId(user.getId(), pageable);
        Page<PaymentResponse> responsePage = payments.map(this::toPaymentResponse);
        return PagedResponse.from(responsePage);
    }

    // Admin methods

    /**
     * Get all payments (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);
        Page<PaymentResponse> responsePage = payments.map(this::toPaymentResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get payment by ID (admin - any payment).
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentAdmin(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithTransactions(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return toPaymentResponse(payment);
    }

    // Helper methods

    private String generatePaymentNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + datePart + "-" + uniquePart;
    }

    private String generateGatewayTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        List<PaymentResponse.TransactionResponse> txnResponses = payment.getTransactions().stream()
                .map(txn -> PaymentResponse.TransactionResponse.builder()
                        .id(txn.getId())
                        .transactionType(txn.getTransactionType())
                        .status(txn.getStatus())
                        .amount(txn.getAmount())
                        .createdAt(txn.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .gatewayName(payment.getGatewayName())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .failureReason(payment.getFailureReason())
                .refundAmount(payment.getRefundAmount())
                .refundReason(payment.getRefundReason())
                .refundedAt(payment.getRefundedAt())
                .transactions(txnResponses)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
