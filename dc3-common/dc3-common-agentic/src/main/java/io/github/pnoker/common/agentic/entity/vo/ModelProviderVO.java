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
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for agentic model provider API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Model Provider view object")
public class ModelProviderVO extends BaseVO {

    @Schema(description = "Provider name")
    private String name;

    @Schema(description = "Provider type", example = "OPENAI_COMPATIBLE")
    private AgenticModelProviderTypeEnum providerType;

    @Schema(description = "Provider API base URL", example = "https://api.openai.com/v1")
    private String baseUrl;

    @Schema(description = "Default provider flag enum (DEFAULT or NON_DEFAULT)", example = "DEFAULT")
    private DefaultFlagEnum defaultFlag;

    @Schema(description = "Enable flag enum (ENABLE or DISABLE)", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    @Schema(description = "Tenant ID", example = "1024")
    private Long tenantId;

}
