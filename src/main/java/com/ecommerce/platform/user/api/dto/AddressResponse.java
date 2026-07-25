package com.ecommerce.platform.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault;
    private String fullAddress;
    private LocalDateTime createdAt;
}
