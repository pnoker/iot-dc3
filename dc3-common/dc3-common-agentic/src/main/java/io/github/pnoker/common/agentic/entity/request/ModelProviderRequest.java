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

import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Agentic model provider mutation request.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
public class ModelProviderRequest {

    @NotNull(message = "Provider ID is required", groups = {Update.class})
    private Long id;

    @NotBlank(message = "Provider name is required", groups = {Add.class, Update.class})
    private String name;

    private String providerType;

    @NotBlank(message = "Provider base URL is required", groups = {Add.class, Update.class})
    private String baseUrl;

    private String apiKey;

    @Min(value = 0, message = "Default flag must be 0 or 1", groups = {Add.class, Update.class})
    @Max(value = 1, message = "Default flag must be 0 or 1", groups = {Add.class, Update.class})
    private Byte defaultFlag;

    @Min(value = 0, message = "Enable flag must be 0 or 1", groups = {Add.class, Update.class})
    @Max(value = 1, message = "Enable flag must be 0 or 1", groups = {Add.class, Update.class})
    private Byte enableFlag;

    private String remark;

}
