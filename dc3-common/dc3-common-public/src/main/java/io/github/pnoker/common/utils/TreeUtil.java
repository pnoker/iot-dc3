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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tree structure builder from flat lists.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public final class TreeUtil {

    private TreeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Build a tree structure using a two-level loop.
     *
     * @param <T>       Object type extending {@link TreeNode}
     * @param treeNodes List of tree nodes to be processed
     * @param root      Root node identifier
     * @return T Tree
     */
    public static <T extends TreeNode> List<T> buildByLoop(List<T> treeNodes, Object root) {
        List<T> trees = new ArrayList<>(16);
        for (T treeNode : treeNodes) {
            if (root.equals(treeNode.getParentId())) {
                trees.add(treeNode);
            }
            for (T it : treeNodes) {
                if (it.getParentId() == treeNode.getId()) {
                    if (Objects.isNull(treeNode.getChildren())) {
                        treeNode.setChildren(new ArrayList<>(16));
                    }
                    treeNode.add(it);
                }
            }
        }
        return trees;
    }

    /**
     * Build a tree structure using recursive calls.
     *
     * @param <T>       Object type extending {@link TreeNode}
     * @param treeNodes List of tree nodes to be processed
     * @param root      Root node identifier
     * @return T Tree
     */
    public static <T extends TreeNode> List<T> buildByRecursive(List<T> treeNodes, Object root) {
        List<T> trees = new ArrayList<>(16);
        for (T treeNode : treeNodes) {
            if (root.equals(treeNode.getParentId())) {
                trees.add(findChildren(treeNode, treeNodes));
            }
        }
        return trees;
    }

    /**
     * Recursively find and attach child nodes.
     *
     * @param <T>       Object type extending {@link TreeNode}
     * @param treeNode  Current node
     * @param treeNodes List of all tree nodes
     * @return T Tree
     */
    public static <T extends TreeNode> T findChildren(T treeNode, List<T> treeNodes) {
        for (T it : treeNodes) {
            if (treeNode.getId() == it.getParentId()) {
                if (Objects.isNull(treeNode.getChildren())) {
                    treeNode.setChildren(new ArrayList<>(16));
                }
                treeNode.add(findChildren(it, treeNodes));
            }
        }
        return treeNode;
    }

}
