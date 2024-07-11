/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
