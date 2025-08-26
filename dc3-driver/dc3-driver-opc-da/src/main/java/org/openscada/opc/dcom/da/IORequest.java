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

package org.openscada.opc.dcom.da;

public class IORequest {
    private String itemID;

    private int maxAge;

    public IORequest(final String itemID, final int maxAge) {
        this.itemID = itemID;
        this.maxAge = maxAge;
    }

    public String getItemID() {
        return this.itemID;
    }

    public void setItemID(final String itemID) {
        this.itemID = itemID;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    public void setMaxAge(final int maxAge) {
        this.maxAge = maxAge;
    }

}