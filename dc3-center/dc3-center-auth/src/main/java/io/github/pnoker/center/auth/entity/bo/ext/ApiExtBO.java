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

package io.github.pnoker.center.auth.entity.bo.ext;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Api BO
 * ApiExt BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
public class ApiExtBO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Api接口编号，一般为URL的MD5编码
     */
    private String title;

    /**
     * Api接口拓展信息
     */
    private String url;

}
