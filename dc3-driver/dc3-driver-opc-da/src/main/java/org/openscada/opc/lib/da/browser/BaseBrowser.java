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

import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.dcom.common.impl.EnumString;
import org.openscada.opc.dcom.da.OPCBROWSETYPE;
import org.openscada.opc.dcom.da.impl.OPCBrowseServerAddressSpace;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.EnumSet;

/**
 * A class implementing base browsing
 *
 * @author Jens Reimann
 */
@Slf4j
public class BaseBrowser {

    protected OPCBrowseServerAddressSpace _browser;

    /**
     * The batch size is the number of entries that will be requested with one call
     * from the server. Sometimes too big batch sizes will cause an exception. And
     * smaller batch sizes degrade perfomance. The default is set by {@link EnumString#DEFAULT_BATCH_SIZE}
     * and can be overridden by the java property openscada.dcom.enum-batch-size.
     */
    protected int _batchSize;

    public BaseBrowser(final OPCBrowseServerAddressSpace browser) {
        this(browser, EnumString.DEFAULT_BATCH_SIZE);
    }

    public BaseBrowser(final OPCBrowseServerAddressSpace browser, final int batchSize) {
        super();
        this._browser = browser;
        this._batchSize = batchSize;
    }

    /**
     * Get the batch size
     *
     * @return the current batch size
     */
    public int getBatchSize() {
        return this._batchSize;
    }

    /**
     * Set the batch size
     *
     * @param batchSize The new batch size
     */
    public void setBatchSize(final int batchSize) {
        this._batchSize = batchSize;
    }

    /**
     * Perform the browse operation.
     *
     * @param type           OPCBROWSETYPE
     * @param filterCriteria Filter Criteria
     * @param accessMask     Access Mask
     * @param variantType    Variant Type
     * @return The browse result
     * @throws IllegalArgumentException IllegalArgumentException
     * @throws UnknownHostException     UnknownHostException
     * @throws JIException              JIException
     */
    protected Collection<String> browse(final OPCBROWSETYPE type, final String filterCriteria, final EnumSet<Access> accessMask, final int variantType) throws IllegalArgumentException, UnknownHostException, JIException {
        int accessMaskValue = 0;

        if (accessMask.contains(Access.READ)) {
            accessMaskValue |= Access.READ.getCode();
        }
        if (accessMask.contains(Access.WRITE)) {
            accessMaskValue |= Access.WRITE.getCode();
        }

        log.debug("Browsing with a batch size of " + this._batchSize);

        return this._browser.browse(type, filterCriteria, accessMaskValue, variantType).asCollection(this._batchSize);
    }

    /**
     * Browse the access paths for one item.
     *
     * @param itemId The item ID to look up the access paths
     * @return The collection of the access paths
     * @throws JIException              JIException
     * @throws UnknownHostException     UnknownHostException
     * @throws IllegalArgumentException IllegalArgumentException
     */
    public Collection<String> getAccessPaths(final String itemId) throws IllegalArgumentException, UnknownHostException, JIException {
        return this._browser.browseAccessPaths(itemId).asCollection(this._batchSize);
    }

}