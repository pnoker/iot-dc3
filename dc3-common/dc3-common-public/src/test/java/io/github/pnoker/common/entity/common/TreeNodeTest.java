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

package io.github.pnoker.common.entity.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TreeNodeTest {

    @Test
    void defaultChildrenListIsEmptyAndMutable() {
        TreeNode node = new TreeNode();
        assertThat(node.getChildren()).isEmpty();
    }

    @Test
    void addAppendsChildToCollection() {
        TreeNode parent = new TreeNode();
        parent.setId(1);
        TreeNode child = new TreeNode();
        child.setId(2);
        child.setParentId(1);

        parent.add(child);

        assertThat(parent.getChildren()).hasSize(1).first().isSameAs(child);
    }

    @Test
    void settersUpdateIdAndParentId() {
        TreeNode node = new TreeNode();
        node.setId(7);
        node.setParentId(3);
        assertThat(node.getId()).isEqualTo(7);
        assertThat(node.getParentId()).isEqualTo(3);
    }
}
