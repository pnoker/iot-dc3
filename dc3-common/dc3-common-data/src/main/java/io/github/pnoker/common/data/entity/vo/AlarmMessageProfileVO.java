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

package io.github.pnoker.common.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.AlarmMessageExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

/**
 * <p>
 * 报警信息模板表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AlarmMessageProfileVO extends BaseVO {

    /**
     * 报警信息模板标题
     */
    private String alarmMessageTitle;

    /**
     * 报警信息模板编号
     */
    private String alarmMessageCode;

    /**
     * 报警信息模板等级
     */
    private AlarmMessageLevelFlagEnum alarmMessageLevel;

    /**
     * 报警信息模板内容
     */
    private AlarmMessageExt alarmMessageExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
