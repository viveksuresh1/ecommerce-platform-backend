package com.ecommerce.platform.user.application.service;

import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.api.dto.AddressResponse;
import com.ecommerce.platform.user.api.dto.CreateAddressRequest;
import com.ecommerce.platform.user.api.dto.UpdateAddressRequest;
import com.ecommerce.platform.user.api.mapper.UserMapper;
import com.ecommerce.platform.user.domain.model.Address;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.AddressRepository;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user address operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Get all addresses for a user.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        return userMapper.toAddressResponseList(addresses);
    }

    /**
     * Get a specific address by ID (must belong to user).
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUser(addressId, userId);
        return userMapper.toAddressResponse(address);
    }

    /**
     * Create a new address for a user.
     */
    @Transactional
    public AddressResponse createAddress(Long userId, CreateAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // If this is the first address or marked as default, handle default logic
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultAddress(userId);
        }

        // If this is the first address, make it default
        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;

        Address address = Address.builder()
                .user(user)
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(isFirstAddress || Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        address = addressRepository.save(address);
        log.info("Address created for user {}: {}", userId, address.getId());

        return userMapper.toAddressResponse(address);
    }

    /**
     * Update an existing address.
     */
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest request) {
        Address address = findAddressByIdAndUser(addressId, userId);

        // Update only provided fields
        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }

        // Handle default flag
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            addressRepository.clearDefaultAddress(userId);
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        log.info("Address updated: {}", addressId);

        return userMapper.toAddressResponse(address);
    }

    /**
     * Delete an address.
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUser(addressId, userId);

        boolean wasDefault = address.getIsDefault();
        addressRepository.delete(address);

        // If deleted address was default, make another address default
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }

        log.info("Address deleted: {}", addressId);
    }

    /**
     * Set an address as default.
     */
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUser(addressId, userId);

        addressRepository.clearDefaultAddress(userId);
        address.setIsDefault(true);
        address = addressRepository.save(address);

        log.info("Default address set: {}", addressId);
        return userMapper.toAddressResponse(address);
    }

    /**
     * Find address by ID and verify it belongs to user.
     */
    private Address findAddressByIdAndUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }
}
