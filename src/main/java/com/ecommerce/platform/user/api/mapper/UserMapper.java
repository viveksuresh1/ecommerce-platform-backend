package com.ecommerce.platform.user.api.mapper;

import com.ecommerce.platform.user.api.dto.AddressResponse;
import com.ecommerce.platform.user.api.dto.UserProfileResponse;
import com.ecommerce.platform.user.domain.model.Address;
import com.ecommerce.platform.user.domain.model.Role;
import com.ecommerce.platform.user.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for User and Address entities to DTOs.
 */
@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .fullAddress(address.getFullAddress())
                .createdAt(address.getCreatedAt())
                .build();
    }

    public List<AddressResponse> toAddressResponseList(List<Address> addresses) {
        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }
}
