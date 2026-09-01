package io.github.pnoker.common.manager.entity.operation;

import java.util.List;

public record DeviceImportManifest(
        int schemaVersion,
        Long driverId,
        Long profileId,
        List<AttributeColumn> driverAttributes,
        List<PointColumn> points) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public record AttributeColumn(Long id, String name) {
    }

    public record PointColumn(Long id, String name, List<AttributeColumn> attributes) {
    }
}
