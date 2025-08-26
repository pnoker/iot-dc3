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

public class PropertyDescription {
    private int _id = -1;

    private String _description = "";

    private short _varType = 0;

    public String getDescription() {
        return this._description;
    }

    public void setDescription(final String description) {
        this._description = description;
    }

    public int getId() {
        return this._id;
    }

    public void setId(final int id) {
        this._id = id;
    }

    public short getVarType() {
        return this._varType;
    }

    public void setVarType(final short varType) {
        this._varType = varType;
    }
}
