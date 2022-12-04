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

package io.github.pnoker.common.model;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import io.github.pnoker.common.enums.PointValueTypeEnum;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 设备变量表
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Point extends BaseModel {

    @NotBlank(message = "Name can't be empty",
            groups = {Insert.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "Invalid name,contains invalid characters or length is not in the range of 2~32",
            groups = {Insert.class, Update.class})
    private String name;

    private String type;
    private Short rw;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Float base;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Float minimum;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Float maximum;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Float multiple;

    private Boolean accrue;
    private Integer decimal;
    private String unit;

    private Boolean enable;

    @NotNull(message = "Profile id can't be empty", groups = {Insert.class, Update.class})
    private String profileId;

    // TODO 后期再实现分组，先放着占个坑 @NotNull(message = "group id can't be empty", groups = {Insert.class, Update.class})
    private String groupId;

    private String tenantId;

    /**
     * 设置默认值
     */
    public void setDefault() {
        this.type = PointValueTypeEnum.STRING.getCode();
        this.rw = 0;
        this.base = 0F;
        this.minimum = -999999F;
        this.maximum = 999999F;
        this.multiple = 1F;
        this.accrue = false;
        this.decimal = 3;
        this.unit = "";
    }
}
