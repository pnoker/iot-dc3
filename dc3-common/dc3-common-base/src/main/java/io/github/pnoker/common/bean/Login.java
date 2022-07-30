/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.common.bean;

import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Check;
import io.github.pnoker.common.valid.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * Login
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Login {

    @NotBlank(message = "Tenant can't be empty", groups = {Auth.class})
    private String tenant;

    @NotBlank(message = "Name can't be empty", groups = {Check.class, Auth.class, Update.class})
    private String name;

    @NotBlank(message = "Salt can't be empty", groups = {Check.class, Auth.class})
    private String salt;

    @NotBlank(message = "Password can't be empty", groups = {Auth.class})
    private String password;

    @NotBlank(message = "Token can't be empty", groups = {Check.class})
    private String token;

}
