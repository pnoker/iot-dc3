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

public class Leaf {
    private Branch _parent = null;

    private String _name = "";

    private String _itemId = null;

    public Leaf(final Branch parent, final String name) {
        this._parent = parent;
        this._name = name;
    }

    public Leaf(final Branch parent, final String name, final String itemId) {
        this._parent = parent;
        this._name = name;
        this._itemId = itemId;
    }

    public String getItemId() {
        return this._itemId;
    }

    public void setItemId(final String itemId) {
        this._itemId = itemId;
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

}
