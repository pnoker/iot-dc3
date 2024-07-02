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

package io.github.pnoker.common.entity.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree node
 *
 * @author pnoker
 * @since 2022.1.0
 */

@Getter
@Setter
public class TreeNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int id;
    private int parentId;
    private List<TreeNode> children = new ArrayList<>(16);

    /**
     * Add
     *
     * @param node TreeNode
     */
    public void add(TreeNode node) {
        children.add(node);
    }
}
