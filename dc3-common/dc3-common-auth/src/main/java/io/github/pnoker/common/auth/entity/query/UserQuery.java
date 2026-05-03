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

package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * User Query
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * Tenant scope. Populated by the controller from the request context;
     * any value supplied by the client is overwritten so a caller cannot
     * reach across tenants.
     */
    private Long tenantId;

    /**
     *
     */
    private String nickName;

    /**
     * Name
     */
    private String userName;

    /**
     *
     */
    private String phone;

    /**
     *
     */
    private String email;

    /**
     * Enable flag, 0 enabled, 1 disabled
     */
    private Byte enableFlag;
}