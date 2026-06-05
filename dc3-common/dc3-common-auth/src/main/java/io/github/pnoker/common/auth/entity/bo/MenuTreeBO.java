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

package io.github.pnoker.common.auth.entity.bo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tree-shaped variant of {@link MenuBO} — nested children mirror parent_menu_id.
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
public class MenuTreeBO extends MenuBO {

    @ToString.Exclude
    private List<MenuTreeBO> children = new ArrayList<>();

    public static MenuTreeBO fromBO(MenuBO source) {
        MenuTreeBO node = new MenuTreeBO();
        node.setId(source.getId());
        node.setParentMenuId(source.getParentMenuId());
        node.setMenuTypeFlag(source.getMenuTypeFlag());
        node.setMenuName(source.getMenuName());
        node.setMenuCode(source.getMenuCode());
        node.setMenuLevel(source.getMenuLevel());
        node.setMenuIndex(source.getMenuIndex());
        node.setMenuExt(source.getMenuExt());
        node.setEnableFlag(source.getEnableFlag());
        node.setRemark(source.getRemark());
        node.setCreatorId(source.getCreatorId());
        node.setCreatorName(source.getCreatorName());
        node.setCreateTime(source.getCreateTime());
        node.setOperatorId(source.getOperatorId());
        node.setOperatorName(source.getOperatorName());
        node.setOperateTime(source.getOperateTime());
        return node;
    }

    public void addChild(MenuTreeBO child) {
        if (Objects.isNull(children)) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

}
