/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.entity.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Device import template context")
public record DeviceImportTemplateRequest(
        @NotNull @Schema(description = "Driver identifier", example = "1024") Long driverId,
        @NotNull @Schema(description = "Profile identifier", example = "2048") Long profileId) {
}
