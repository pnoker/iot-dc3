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
 * @author Jens Reimann <jens.reimann@th4-systems.com>
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
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
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
