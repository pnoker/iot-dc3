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

package org.openscada.opc.dcom.common;

import java.io.Serial;
import java.util.ArrayList;

public class KeyedResultSet<K, V> extends ArrayList<KeyedResult<K, V>> {

    @Serial
    private static final long serialVersionUID = 1L;

    public KeyedResultSet() {
        super();
    }

    public KeyedResultSet(final int size) {
        super(size); // me
    }
}
