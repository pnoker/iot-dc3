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
package io.github.pnoker.common.agentic.entity.vo;

import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for agentic model configuration API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Model Config view object")
public class ModelConfigVO extends BaseVO {

    @Schema(description = "model")

    private String model;

    @Schema(description = "label")

    private String label;

    @Schema(description = "provider ID")

    private Long providerId;

    @Schema(description = "provider name")

    private String providerName;

    @Schema(description = "stream")

    private Boolean stream;

    @Schema(description = "tool call")

    private Boolean toolCall;

    @Schema(description = "vision")

    private Boolean vision;

    @Schema(description = "reasoning")

    private Boolean reasoning;

    @Schema(description = "temperature")

    private Double temperature;

    @Schema(description = "max tokens")

    private Integer maxTokens;

    @Schema(description = "Default flag")

    private DefaultFlagEnum defaultFlag;

    @Schema(description = "Enable flag: 0=enabled, 1=disabled")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Tenant ID")

    private Long tenantId;

}
