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

package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.base.BaseVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Point BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
@RequiredArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "Point", description = "位号")
public class PointVO extends BaseVO {

    /**
     * 位号名称
     */
    @Schema(description = "位号名称")
    @NotBlank(message = "位号名称不能为空",
            groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "位号名称格式无效",
            groups = {Add.class, Update.class})
    private String pointName;

    /**
     * 位号编号
     */
    @Schema(description = "位号编号")
    private String pointCode;

    /**
     * 位号类型标识
     */
    @Schema(description = "位号类型标识")
    private PointTypeFlagEnum pointTypeFlag;

    /**
     * 读写标识
     */
    @Schema(description = "读写标识")
    private RwFlagEnum rwFlag;

    /**
     * 基础值
     */
    @Schema(description = "基础值")
    private BigDecimal baseValue;

    /**
     * 比例系数
     */
    @Schema(description = "比例系数")
    private BigDecimal multiple;

    /**
     * 数据精度
     */
    @Schema(description = "数据精度")
    private Byte valueDecimal;

    /**
     * 单位
     */
    @Schema(description = "单位")
    private String unit;

    /**
     * 模板ID
     */
    @Schema(description = "模板ID")
    @NotNull(message = "模版ID不能为空",
            groups = {Add.class, Update.class})
    private Long profileId;

    /**
     * 分组ID
     */
    @Schema(description = "分组ID")
    private Long groupId;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 设置默认值
     */
    public void setDefault() {
        this.pointTypeFlag = PointTypeFlagEnum.STRING;
        this.rwFlag = RwFlagEnum.R;
        this.baseValue = BigDecimal.valueOf(0);
        this.multiple = BigDecimal.valueOf(1);
        this.valueDecimal = 6;
        this.unit = "";
    }

}
