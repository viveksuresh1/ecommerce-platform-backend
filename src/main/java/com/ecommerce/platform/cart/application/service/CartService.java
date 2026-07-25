package com.ecommerce.platform.cart.application.service;

import com.ecommerce.platform.cart.api.dto.AddToCartRequest;
import com.ecommerce.platform.cart.api.dto.CartResponse;
import com.ecommerce.platform.cart.api.dto.UpdateCartItemRequest;
import com.ecommerce.platform.cart.domain.model.Cart;
import com.ecommerce.platform.cart.domain.model.CartItem;
import com.ecommerce.platform.cart.domain.repository.CartItemRepository;
import com.ecommerce.platform.cart.domain.repository.CartRepository;
import com.ecommerce.platform.inventory.domain.model.Inventory;
import com.ecommerce.platform.inventory.domain.repository.InventoryRepository;
import com.ecommerce.platform.product.domain.model.Product;
import com.ecommerce.platform.product.domain.model.ProductImage;
import com.ecommerce.platform.product.domain.model.ProductStatus;
import com.ecommerce.platform.product.domain.repository.ProductRepository;
import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for cart operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    /**
     * Get current user's cart.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        return toCartResponse(cart);
    }

    /**
     * Add item to cart.
     */
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is not available for purchase");
        }

        // Check stock availability
        Inventory inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new BadRequestException("Product is out of stock"));

        int existingQty = cart.findItemByProductId(product.getId())
                .map(CartItem::getQuantity)
                .orElse(0);
        int totalQty = existingQty + request.getQuantity();

        if (!inventory.hasAvailableStock(totalQty)) {
            throw new BadRequestException(
                    String.format("Only %d units available", inventory.getAvailableQuantity()));
        }

        // Add or update item
        CartItem item = CartItem.builder()
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(product.getPrice())
                .build();

        cart.addItem(item);
        cartRepository.save(cart);

        log.info("Added {} x {} to cart for user {}", request.getQuantity(), product.getName(), user.getEmail());
        return toCartResponse(cart);
    }

    /**
     * Update cart item quantity.
     */
    @Transactional
    public CartResponse updateCartItem(Long productId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        CartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));

        // Check stock availability
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BadRequestException("Product is out of stock"));

        if (!inventory.hasAvailableStock(request.getQuantity())) {
            throw new BadRequestException(
                    String.format("Only %d units available", inventory.getAvailableQuantity()));
        }

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(item.getProduct().getPrice()); // Update to current price
        cartRepository.save(cart);

        log.info("Updated cart item {} to qty {} for user {}", productId, request.getQuantity(), user.getEmail());
        return toCartResponse(cart);
    }

    /**
     * Remove item from cart.
     */
    @Transactional
    public CartResponse removeFromCart(Long productId) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", user.getId()));

        cart.removeItem(productId);
        cartRepository.save(cart);

        log.info("Removed product {} from cart for user {}", productId, user.getEmail());
        return toCartResponse(cart);
    }

    /**
     * Clear entire cart.
     */
    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElse(null);

        if (cart != null) {
            cart.clear();
            cartRepository.save(cart);
            log.info("Cleared cart for user {}", user.getEmail());
        }
    }

    /**
     * Get cart item count.
     */
    @Transactional(readOnly = true)
    public int getCartItemCount() {
        User user = getCurrentUser();
        return cartRepository.findByUserIdWithItems(user.getId())
                .map(Cart::getTotalItems)
                .orElse(0);
    }

    // ==================== Helper Methods ====================

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserIdWithItemsAndProducts(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private CartResponse toCartResponse(Cart cart) {
        // Get inventory for all products in cart
        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());

        Map<Long, Inventory> inventoryMap = inventoryRepository.findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    Inventory inventory = inventoryMap.get(product.getId());
                    int availableStock = inventory != null ? inventory.getAvailableQuantity() : 0;

                    ProductImage primaryImage = product.getPrimaryImage();
                    String imageUrl = primaryImage != null ? primaryImage.getUrl() : null;

                    return CartResponse.CartItemResponse.builder()
                            .id(item.getId())
                            .productId(product.getId())
                            .productName(product.getName())
                            .productSlug(product.getSlug())
                            .productImageUrl(imageUrl)
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .availableStock(availableStock)
                            .inStock(availableStock > 0)
                            .build();
                })
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(cart.getTotalItems())
                .subtotal(cart.getSubtotal())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
