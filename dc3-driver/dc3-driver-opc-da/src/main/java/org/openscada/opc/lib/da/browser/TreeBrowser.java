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

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.da.OPCBROWSEDIRECTION;
import org.openscada.opc.dcom.da.OPCBROWSETYPE;
import org.openscada.opc.dcom.da.impl.OPCBrowseServerAddressSpace;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;

/**
 * Browse through the hierarchical server namespace.
 * <p>
 * The operations on the address space browser browser are not synchronized
 * as is the TreeBrowser itself. The user must take care of preventing
 * simultanious access to this instance and the server address space browser.
 *
 * @author Jens Reimann jens.reimann@th4-systems.com
 */
public class TreeBrowser extends BaseBrowser {

    private String _filterCriteria = "";

    private EnumSet<Access> _accessMask = EnumSet.noneOf(Access.class);

    private int _variantType = JIVariant.VT_EMPTY;

    /**
     * Browse for all items without search parameters.
     * <p>
     * This will actually call:
     * <code>
     * TreeBrowser ( browser, "", EnumSet.noneOf ( Access.class ), JIVariant.VT_EMPTY );
     * </code>
     *
     * @param browser The browser to use for browsing
     */
    public TreeBrowser(final OPCBrowseServerAddressSpace browser) {
        super(browser);
    }

    /**
     * Browse for items with search parameters.
     *
     * @param browser        The browser to use
     * @param filterCriteria The filter criteria. It is specific to the server you use.
     * @param accessMask     The access mask (use <code>EnumSet.noneOf ( Access.class )</code> for all)
     * @param variantType    The variant type (use <code>JIVariant.VT_EMPTY</code> for all)
     */
    public TreeBrowser(final OPCBrowseServerAddressSpace browser, final String filterCriteria, final EnumSet<Access> accessMask, final int variantType) {
        super(browser);
        this._filterCriteria = filterCriteria;
        this._accessMask = accessMask;
        this._variantType = variantType;
    }

    /**
     * Move the tree browser to the root folder
     *
     * @throws JIException JIException
     */
    protected void moveToRoot() throws JIException {
        this._browser.changePosition(null, OPCBROWSEDIRECTION.OPC_BROWSE_TO);
    }

    /**
     * Move the tree browser to a branch
     *
     * @param branch The branch to move to
     * @throws JIException JIException
     */
    protected void moveToBranch(final Branch branch) throws JIException {
        Collection<String> branchStack = branch.getBranchStack();

        moveToRoot();
        for (String branchName : branchStack) {
            this._browser.changePosition(branchName, OPCBROWSEDIRECTION.OPC_BROWSE_DOWN);
        }
    }

    /**
     * Browse the root branch for its sub-branches.
     *
     * @return The list of sub-branches
     * @throws JIException              JIException
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     */
    public Branch browseBranches() throws JIException, IllegalArgumentException, UnknownHostException {
        Branch branch = new Branch();
        fillBranches(branch);
        return branch;
    }

    /**
     * Browse the root branch for this leaves.
     *
     * @return The list of leaves
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     * @throws JIException              JIException
     */
    public Branch browseLeaves() throws IllegalArgumentException, UnknownHostException, JIException {
        Branch branch = new Branch();
        fillLeaves(branch);
        return branch;
    }

    /**
     * Fill the branch list of the provided branch.
     *
     * @param branch The branch to fill.
     * @throws JIException              JIException
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     */
    public void fillBranches(final Branch branch) throws JIException, IllegalArgumentException, UnknownHostException {
        moveToBranch(branch);
        browse(branch, false, true, false);
    }

    /**
     * Fill the leaf list of the provided branch.
     *
     * @param branch The branch to fill.
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     * @throws JIException              JIException
     */
    public void fillLeaves(final Branch branch) throws IllegalArgumentException, UnknownHostException, JIException {
        moveToBranch(branch);
        browse(branch, true, false, false);
    }

    /**
     * Browse through all levels of the tree browser.
     *
     * @return The whole expanded server address space
     * @throws JIException              JIException
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     */
    public Branch browse() throws JIException, IllegalArgumentException, UnknownHostException {
        Branch branch = new Branch();
        fill(branch);
        return branch;
    }

    /**
     * Fill the leaves and branches of the branch provided branches including
     * alls sub-branches.
     *
     * @param branch The branch to fill.
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     * @throws JIException              JIException
     */
    public void fill(final Branch branch) throws IllegalArgumentException, UnknownHostException, JIException {
        moveToBranch(branch);
        browse(branch, true, true, true);
    }

    /**
     * Fill the branch object with the leaves of this currently selected branch.
     * <p>
     * The server object is not located to the branch before browsing!
     *
     * @param branch The branch to fill
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     * @throws JIException              JIException
     */
    protected void browseLeaves(final Branch branch) throws IllegalArgumentException, UnknownHostException, JIException {
        branch.setLeaves(new LinkedList<Leaf>());

        for (String item : browse(OPCBROWSETYPE.OPC_LEAF, this._filterCriteria, this._accessMask, this._variantType)) {
            Leaf leaf = new Leaf(branch, item, this._browser.getItemID(item));
            branch.getLeaves().add(leaf);
        }
    }

    protected void browseBranches(final Branch branch, final boolean leaves, final boolean descend) throws IllegalArgumentException, UnknownHostException, JIException {
        branch.setBranches(new LinkedList<Branch>());

        for (String item : browse(OPCBROWSETYPE.OPC_BRANCH, this._filterCriteria, this._accessMask, this._variantType)) {
            Branch subBranch = new Branch(branch, item);
            // descend only if we should
            if (descend) {
                this._browser.changePosition(item, OPCBROWSEDIRECTION.OPC_BROWSE_DOWN);
                browse(subBranch, leaves, true, true);
                this._browser.changePosition(null, OPCBROWSEDIRECTION.OPC_BROWSE_UP);
            }
            branch.getBranches().add(subBranch);
        }
    }

    protected void browse(final Branch branch, final boolean leaves, final boolean branches, final boolean descend) throws IllegalArgumentException, UnknownHostException, JIException {
        // process leaves
        if (leaves) {
            browseLeaves(branch);
        }

        // process branches
        if (branches) {
            browseBranches(branch, leaves, descend);
        }
    }
}
