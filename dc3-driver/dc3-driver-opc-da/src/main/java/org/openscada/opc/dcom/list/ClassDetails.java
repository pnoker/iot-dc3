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

package org.openscada.opc.dcom.list;

/**
 * Details about an OPC server class
 *
 * @author Jens Reimann &lt;jens.reimann@th4-systems.com&gt;
 * @since 0.1.8
 */
public class ClassDetails {
    private String _clsId;

    private String _progId;

    private String _description;

    public String getClsId() {
        return this._clsId;
    }

    public void setClsId(final String clsId) {
        this._clsId = clsId;
    }

    public String getDescription() {
        return this._description;
    }

    public void setDescription(final String description) {
        this._description = description;
    }

    public String getProgId() {
        return this._progId;
    }

    public void setProgId(final String progId) {
        this._progId = progId;
    }
}
