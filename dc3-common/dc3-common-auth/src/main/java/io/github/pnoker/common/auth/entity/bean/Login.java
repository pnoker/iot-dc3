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

package io.github.pnoker.common.auth.entity.bean;

import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Check;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Login
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Login implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "租户不能为空",
            groups = {Auth.class})
    private String tenant;

    @NotBlank(message = "名称不能为空",
            groups = {Check.class, Auth.class, Update.class})
    private String name;

    @NotBlank(message = "盐值不能为空",
            groups = {Check.class, Auth.class})
    private String salt;

    @NotBlank(message = "密码不能为空",
            groups = {Auth.class})
    private String password;

    @NotBlank(message = "令牌不能为空",
            groups = {Check.class})
    private String token;

}
