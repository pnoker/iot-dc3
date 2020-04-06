/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * OpenSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * OpenSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with OpenSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
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
