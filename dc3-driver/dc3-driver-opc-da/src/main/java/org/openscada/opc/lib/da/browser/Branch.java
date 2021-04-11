/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openscada.opc.lib.da.browser;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class Branch {
    private Branch _parent = null;

    private String _name = null;

    private Collection<Branch> _branches = new LinkedList<Branch>();

    private Collection<Leaf> _leaves = new LinkedList<Leaf>();

    /**
     * Create a branch to the virtual root folder
     */
    public Branch() {
        super();
    }

    /**
     * Create a branch with a parent branch and a name of this branch.
     *
     * @param parent The parent of this branch
     * @param name   The name of this branch
     */
    public Branch(final Branch parent, final String name) {
        super();
        this._name = name;
        this._parent = parent;
    }

    /**
     * Get all branches.
     * <br/>
     * They must be filled first with a fill method from the {@link TreeBrowser}
     *
     * @return The list of branches
     */
    public Collection<Branch> getBranches() {
        return this._branches;
    }

    public void setBranches(final Collection<Branch> branches) {
        this._branches = branches;
    }

    /**
     * Get all leaves.
     * <br/>
     * They must be filled first with a fill method from the {@link TreeBrowser}
     *
     * @return The list of leaves
     */
    public Collection<Leaf> getLeaves() {
        return this._leaves;
    }

    public void setLeaves(final Collection<Leaf> leaves) {
        this._leaves = leaves;
    }

    public String getName() {
        return this._name;
    }

    public void setName(final String name) {
        this._name = name;
    }

    public Branch getParent() {
        return this._parent;
    }

    /**
     * Get the list of names from the parent up to this branch
     *
     * @return The stack of branch names from the parent up this one
     */
    public Collection<String> getBranchStack() {
        LinkedList<String> branches = new LinkedList<String>();

        Branch currentBranch = this;
        while (currentBranch.getParent() != null) {
            branches.add(currentBranch.getName());
            currentBranch = currentBranch.getParent();
        }

        Collections.reverse(branches);
        return branches;
    }

}
