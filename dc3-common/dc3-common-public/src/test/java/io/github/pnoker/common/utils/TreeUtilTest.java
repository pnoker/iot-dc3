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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.entity.common.TreeNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TreeUtilTest {

    private static TreeNode node(int id, int parentId) {
        TreeNode node = new TreeNode();
        node.setId(id);
        node.setParentId(parentId);
        return node;
    }

    @Test
    void buildByLoopReturnsEmptyForEmptyInput() {
        assertThat(TreeUtil.buildByLoop(List.of(), 0)).isEmpty();
    }

    @Test
    void buildByLoopAttachesChildrenToRoots() {
        TreeNode root = node(1, 0);
        TreeNode child = node(2, 1);
        TreeNode grandchild = node(3, 2);
        List<TreeNode> roots = TreeUtil.buildByLoop(List.of(root, child, grandchild), 0);
        assertThat(roots).extracting(TreeNode::getId).containsExactly(1);
        assertThat(roots.get(0).getChildren()).extracting(TreeNode::getId).contains(2);
    }

    @Test
    void buildByLoopReturnsMultipleRoots() {
        List<TreeNode> roots = TreeUtil.buildByLoop(List.of(node(1, 0), node(2, 0)), 0);
        assertThat(roots).extracting(TreeNode::getId).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void buildByRecursiveReturnsEmptyForEmptyInput() {
        assertThat(TreeUtil.buildByRecursive(List.of(), 0)).isEmpty();
    }

    @Test
    void buildByRecursiveAttachesChildrenAndGrandchildren() {
        TreeNode root = node(1, 0);
        TreeNode child = node(2, 1);
        TreeNode grandchild = node(3, 2);
        List<TreeNode> roots = TreeUtil.buildByRecursive(List.of(root, child, grandchild), 0);
        assertThat(roots).hasSize(1);
        TreeNode rootNode = roots.get(0);
        assertThat(rootNode.getChildren()).extracting(TreeNode::getId).contains(2);
        TreeNode childNode = rootNode.getChildren().get(0);
        assertThat(childNode.getChildren()).extracting(TreeNode::getId).contains(3);
    }

    @Test
    void findChildrenAttachesChildrenAndReturnsTheRoot() {
        TreeNode root = node(1, 0);
        TreeNode child = node(2, 1);
        TreeNode result = TreeUtil.findChildren(root, List.of(child));
        assertThat(result).isSameAs(root);
        assertThat(result.getChildren()).extracting(TreeNode::getId).contains(2);
    }

    @Test
    void buildByLoopFiltersNonMatchingRoots() {
        List<TreeNode> roots = TreeUtil.buildByLoop(List.of(node(1, 5), node(2, 5)), 0);
        assertThat(roots).isEmpty();
    }
}
