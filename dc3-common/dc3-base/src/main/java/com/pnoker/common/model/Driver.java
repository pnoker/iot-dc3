/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.model;

import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 设备驱动表
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Driver extends Description {

    @NotBlank(message = "name can't be empty", groups = {Insert.class})
    @Pattern(regexp = "^[a-zA-Z]\\w{2,32}$", message = "invalid name , /^[a-zA-Z]\\w{2,32}$/", groups = {Insert.class, Update.class})
    private String name;

    @NotBlank(message = "service name can't be empty", groups = {Insert.class})
    @Pattern(regexp = "^[a-zA-Z]\\w{2,32}$", message = "invalid service name , /^[a-zA-Z]\\w{2,32}$/", groups = {Insert.class, Update.class})
    private String serviceName;

    private String connectInfo;
    private String profileInfo;
}
