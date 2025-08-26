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
     * <p>
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
     * <p>
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
