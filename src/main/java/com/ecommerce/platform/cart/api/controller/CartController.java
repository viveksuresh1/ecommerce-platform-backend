package com.ecommerce.platform.cart.api.controller;

import com.ecommerce.platform.cart.api.dto.AddToCartRequest;
import com.ecommerce.platform.cart.api.dto.CartResponse;
import com.ecommerce.platform.cart.api.dto.UpdateCartItemRequest;
import com.ecommerce.platform.cart.application.service.CartService;
import com.ecommerce.platform.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cart endpoints - all require authentication.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart", description = "Get current user's shopping cart")
    public ApiResponse<CartResponse> getCart() {
        CartResponse cart = cartService.getCart();
        return ApiResponse.success(cart);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add to cart", description = "Add product to cart")
    public ApiResponse<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartResponse cart = cartService.addToCart(request);
        return ApiResponse.success(cart, "Item added to cart");
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item", description = "Update quantity of item in cart")
    public ApiResponse<CartResponse> updateCartItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse cart = cartService.updateCartItem(productId, request);
        return ApiResponse.success(cart, "Cart updated");
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove from cart", description = "Remove product from cart")
    public ApiResponse<CartResponse> removeFromCart(@PathVariable Long productId) {
        CartResponse cart = cartService.removeFromCart(productId);
        return ApiResponse.success(cart, "Item removed from cart");
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from cart")
    public ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.success(null, "Cart cleared");
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count", description = "Get total number of items in cart")
    public ApiResponse<Map<String, Integer>> getCartItemCount() {
        int count = cartService.getCartItemCount();
        return ApiResponse.success(Map.of("count", count));
    }
}
