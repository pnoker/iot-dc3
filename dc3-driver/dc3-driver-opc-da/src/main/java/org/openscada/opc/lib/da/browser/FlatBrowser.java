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
import org.openscada.opc.dcom.da.OPCBROWSETYPE;
import org.openscada.opc.dcom.da.impl.OPCBrowseServerAddressSpace;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Browse through the flat server namespace
 *
 * @author Jens Reimann jens.reimann@th4-systems.com
 */
public class FlatBrowser extends BaseBrowser {
    public FlatBrowser(final OPCBrowseServerAddressSpace browser) {
        super(browser);
    }

    public FlatBrowser(final OPCBrowseServerAddressSpace browser, final int batchSize) {
        super(browser, batchSize);
    }

    /**
     * Perform a flat browse operation
     *
     * @param filterCriteria The filter criteria. Use an empty string if you don't need one.
     * @param accessMask     The access mask. An empty set will search for all.
     * @param variantType    The variant type. Must be one of the <code>VT_</code> constants of {@link JIVariant}. Use {@link JIVariant#VT_EMPTY} if you want to browse for all.
     * @return The list of entries
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     * @throws JIException              JIException
     */
    public Collection<String> browse(final String filterCriteria, final EnumSet<Access> accessMask, final int variantType) throws IllegalArgumentException, UnknownHostException, JIException {
        return browse(OPCBROWSETYPE.OPC_FLAT, filterCriteria, accessMask, variantType);
    }

    public Collection<String> browse(final String filterCriteria) throws IllegalArgumentException, UnknownHostException, JIException {
        return browse(filterCriteria, EnumSet.noneOf(Access.class), JIVariant.VT_EMPTY);
    }

    public Collection<String> browse() throws IllegalArgumentException, UnknownHostException, JIException {
        return browse("", EnumSet.noneOf(Access.class), JIVariant.VT_EMPTY);
    }

    public Collection<String> browse(final EnumSet<Access> accessMask) throws IllegalArgumentException, UnknownHostException, JIException {
        return browse("", accessMask, JIVariant.VT_EMPTY);
    }

}
