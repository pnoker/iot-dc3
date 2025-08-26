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

package io.github.pnoker.common.dal.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * LabelBind VO
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
public class LabelBindVO extends BaseVO {

    /**
     * 实体类型标识
     */
    @NotNull(message = "实体类型标识不能为空",
            groups = {Add.class, Update.class})
    private EntityTypeFlagEnum entityTypeFlag;

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空",
            groups = {Add.class, Update.class})
    private Long labelId;

    /**
     * 实体ID
     */
    @NotNull(message = "实体ID不能为空",
            groups = {Add.class, Update.class})
    private Long entityId;
}
