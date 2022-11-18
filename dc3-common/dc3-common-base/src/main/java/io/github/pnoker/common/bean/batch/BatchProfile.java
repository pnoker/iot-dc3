/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.bean.batch;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class BatchProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Boolean share;
    private Short type;

    private Map<String, String> driverConfig;

    private List<BatchPoint> points;
    /**
     * 仅当share为true的时候生效
     */
    private Map<String, Map<String, String>> pointConfig;

    private List<BatchGroup> groups;
}
