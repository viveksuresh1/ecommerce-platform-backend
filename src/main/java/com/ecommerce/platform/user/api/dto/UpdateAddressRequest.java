package com.ecommerce.platform.user.api.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an address.
 * All fields are optional - only provided fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {

    @Size(max = 255, message = "Street must be less than 255 characters")
    private String street;

    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;

    @Size(max = 100, message = "State must be less than 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must be less than 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must be less than 100 characters")
    private String country;

    private Boolean isDefault;
}
