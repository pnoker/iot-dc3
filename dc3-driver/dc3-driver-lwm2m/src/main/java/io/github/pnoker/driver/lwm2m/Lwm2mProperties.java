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

package io.github.pnoker.driver.lwm2m;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * LwM2M Properties Configuration Class
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.driver.lwm2m")
public class Lwm2mProperties {

    @NotNull(message = "Server host can't be null")
    private String serverHost = "0.0.0.0";

    @NotNull(message = "Server port can't be null")
    @Min(value = 1, message = "Server port must be greater than 0")
    private Integer serverPort = 5683;

    @NotNull(message = "Secure port can't be null")
    @Min(value = 1, message = "Secure port must be greater than 0")
    private Integer securePort = 5684;

    @NotNull(message = "Security mode can't be null")
    private String securityMode = "NOSEC";

    private String pskIdentity;

    private String pskKey;

}
