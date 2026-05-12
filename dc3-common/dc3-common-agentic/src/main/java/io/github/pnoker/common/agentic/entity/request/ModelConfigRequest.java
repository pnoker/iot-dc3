/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.agentic.entity.request;

import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Agentic model configuration mutation request.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
public class ModelConfigRequest {

    @NotNull(message = "Model config ID is required", groups = {Update.class})
    private Long id;

    @NotBlank(message = "Model is required", groups = {Add.class, Update.class})
    private String model;

    private String label;

    @NotNull(message = "Provider is required", groups = {Add.class, Update.class})
    private Long providerId;

    private Boolean stream;

    private Boolean toolCall;

    private Boolean vision;

    private Boolean reasoning;

    @DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 2.0",
            groups = {Add.class, Update.class})
    @DecimalMax(value = "2.0", message = "Temperature must be between 0.0 and 2.0",
            groups = {Add.class, Update.class})
    private Double temperature;

    @Min(value = 1, message = "Max tokens must be greater than 0", groups = {Add.class, Update.class})
    private Integer maxTokens;

    private DefaultFlagEnum defaultFlag;

    private EnableFlagEnum enableFlag;

    private String remark;

}
