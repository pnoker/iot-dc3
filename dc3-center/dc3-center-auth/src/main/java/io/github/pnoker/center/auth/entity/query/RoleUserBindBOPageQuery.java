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

package io.github.pnoker.center.auth.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.center.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.common.entity.common.Pages;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author linys
 * @since 2022.1.0
 */
@Data
public class RoleUserBindBOPageQuery extends RoleUserBindBO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;
}
