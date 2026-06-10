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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agentic model provider mutation request.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@Schema(description = "Model Provider request body")
public class ModelProviderRequest {

    @Schema(description = "Primary key")

    @NotNull(message = "Provider ID is required", groups = {Update.class})
    private Long id;

    @Schema(description = "Provider display name")

    @NotBlank(message = "Provider name is required", groups = {Add.class, Update.class})
    private String name;

    @Schema(description = "Provider type")

    private String providerType;

    @Schema(description = "Provider API base URL")

    @NotBlank(message = "Provider base URL is required", groups = {Add.class, Update.class})
    private String baseUrl;

    @Schema(description = "Provider API key")

    private String apiKey;

    @Schema(description = "Default provider flag enum (DEFAULT or NON_DEFAULT)")

    private DefaultFlagEnum defaultFlag;

    @Schema(description = "Enable flag enum (ENABLE or DISABLE)")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Description / remark")

    private String remark;

}
