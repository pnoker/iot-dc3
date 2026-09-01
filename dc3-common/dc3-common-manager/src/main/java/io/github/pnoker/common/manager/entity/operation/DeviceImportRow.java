package io.github.pnoker.common.manager.entity.operation;

import java.util.List;

public record DeviceImportRow(
        int rowNumber,
        String deviceName,
        String remark,
        List<String> driverAttributeValues,
        List<String> pointAttributeValues) {
}
