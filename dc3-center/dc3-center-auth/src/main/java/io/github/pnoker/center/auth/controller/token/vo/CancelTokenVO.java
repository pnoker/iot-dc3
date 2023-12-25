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

package io.github.pnoker.center.auth.controller.token.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * CancelTokenVO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Schema(title = "取消令牌请求体")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CancelTokenVO {

    @NotBlank(message = "租户不能为空")
    @Schema(title = "租户", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenant;

    @NotBlank(message = "用户名不能为空")
    @Schema(title = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, description = "登陆使用的用户名")
    private String name;
}
