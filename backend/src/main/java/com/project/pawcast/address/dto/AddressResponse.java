package com.project.pawcast.address.dto;

import com.project.pawcast.address.entity.Addresses;

public record AddressResponse(
        Long id,
        String label,
        String recipient,
        String tel,
        String zipcode,
        String address1,
        String address2,
        boolean isDefault
) {
    public static AddressResponse from(Addresses addresses) {
        return new AddressResponse(
                addresses.getId(),
                addresses.getLabel(),
                addresses.getRecipient(),
                addresses.getTel(),
                addresses.getZipcode(),
                addresses.getAddress1(),
                addresses.getAddress2(),
                addresses.isDefault()
        );
    }
}
