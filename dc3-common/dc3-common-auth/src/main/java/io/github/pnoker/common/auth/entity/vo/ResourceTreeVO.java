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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-shaped variant of {@link ResourceVO} — adds nested children so the frontend's
 * el-table tree can render {@code row-key="id" :tree-props="{ children: 'children' }"}.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Tree-shaped resource view object used for hierarchical rendering; each node may carry a nested list of child resource nodes.")
public class ResourceTreeVO extends ResourceVO {

    @Schema(description = "Ordered list of direct child resource nodes under this node; empty when the resource is a leaf.")
    @ToString.Exclude
    private List<ResourceTreeVO> children = new ArrayList<>();

}
