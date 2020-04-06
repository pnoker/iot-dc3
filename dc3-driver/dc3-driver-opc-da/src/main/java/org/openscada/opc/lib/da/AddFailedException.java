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

package org.openscada.opc.lib.da;

import java.util.HashMap;
import java.util.Map;

public class AddFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 5299486640366935298L;

    private Map<String, Integer> _errors = new HashMap<String, Integer>();

    private Map<String, Item> _items = new HashMap<String, Item>();

    public AddFailedException(final Map<String, Integer> errors, final Map<String, Item> items) {
        super();
        this._errors = errors;
        this._items = items;
    }

    /**
     * Get the map of item id to error code
     *
     * @return the result map containing the failed items
     */
    public Map<String, Integer> getErrors() {
        return this._errors;
    }

    /**
     * Get the map of item it to item object
     *
     * @return the result map containing the succeeded items
     */
    public Map<String, Item> getItems() {
        return this._items;
    }
}
