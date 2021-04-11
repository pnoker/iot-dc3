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
