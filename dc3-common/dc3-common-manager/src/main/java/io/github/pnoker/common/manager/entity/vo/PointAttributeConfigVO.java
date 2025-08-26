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

package io.github.pnoker.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * PointAttributeConfig BO
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class PointAttributeConfigVO extends BaseVO {

    /**
     * 位号属性ID
     */
    @NotNull(message = "位号属性ID不能为空",
            groups = {Add.class, Update.class})
    private Long attributeId;

    /**
     * 位号属性配置值
     */
    @NotNull(message = "位号属性配置值不能为空")
    private String configValue;

    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空",
            groups = {Add.class, Update.class})
    private Long deviceId;

    /**
     * 位号配置信息
     */
    private JsonExt configExt;

    /**
     * 位号ID
     */
    @NotNull(message = "位号ID不能为空",
            groups = {Add.class, Update.class})
    private Long pointId;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 签名
     */
    private String signature;

    /**
     * 版本
     */
    private Integer version;
}
