package com.project.powcast.product.dto;

import com.project.powcast.product.entity.DeviceDetail;

public record DeviceDetailResponse(
        String deviceKind,
        String material,
        String featureDescription
) {

    public static DeviceDetailResponse from(DeviceDetail detail) {
        if (detail == null) {
            return null;
        }

        return new DeviceDetailResponse(
                detail.getDeviceKind(),
                detail.getMaterial(),
                detail.getFeatureDescription()
        );
    }
}