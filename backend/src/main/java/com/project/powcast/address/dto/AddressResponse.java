package com.project.powcast.address.dto;

import com.project.powcast.address.entity.Addresses;

public record AddressResponse(
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
