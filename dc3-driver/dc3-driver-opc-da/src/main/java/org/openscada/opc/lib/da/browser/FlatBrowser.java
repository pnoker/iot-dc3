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
