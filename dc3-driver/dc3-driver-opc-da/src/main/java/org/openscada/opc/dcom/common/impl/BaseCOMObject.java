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

package org.openscada.opc.dcom.common.impl;

import org.jinterop.dcom.core.IJIComObject;

public class BaseCOMObject {
    private IJIComObject comObject = null;

    /**
     * Create a new base COM object
     *
     * @param comObject The COM object to wrap but be addRef'ed
     */
    public BaseCOMObject(final IJIComObject comObject) {
        this.comObject = comObject;
    }

    protected synchronized IJIComObject getCOMObject() {
        return this.comObject;
    }
}
