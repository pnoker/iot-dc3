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
