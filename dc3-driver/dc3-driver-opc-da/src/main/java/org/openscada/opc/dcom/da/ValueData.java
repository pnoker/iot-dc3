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

package org.openscada.opc.dcom.da;

import org.jinterop.dcom.core.JIVariant;

import java.util.Calendar;

public class ValueData {
    private JIVariant value;

    private short quality;

    private Calendar timestamp;

    public short getQuality() {
        return this.quality;
    }

    public void setQuality(final short quality) {
        this.quality = quality;
    }

    public Calendar getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(final Calendar timestamp) {
        this.timestamp = timestamp;
    }

    public JIVariant getValue() {
        return this.value;
    }

    public void setValue(final JIVariant value) {
        this.value = value;
    }
}
