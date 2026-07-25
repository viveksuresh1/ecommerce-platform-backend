package com.ecommerce.platform.payment.domain.repository;

import com.ecommerce.platform.payment.domain.model.Payment;
import com.ecommerce.platform.payment.domain.model.PaymentTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(@Param("userId") Long userId, Pageable pageable);

    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentTransactionStatus status, Pageable pageable);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    boolean existsByPaymentNumber(String paymentNumber);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.transactions WHERE p.id = :id")
    Optional<Payment> findByIdWithTransactions(@Param("id") Long id);
}
