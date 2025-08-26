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

package org.openscada.opc.lib.da;

import java.util.HashMap;
import java.util.Map;

public class AddFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

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
