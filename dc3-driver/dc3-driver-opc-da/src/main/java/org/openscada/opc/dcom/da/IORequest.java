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

public class IORequest {
    private String itemID;

    private int maxAge;

    public IORequest(final String itemID, final int maxAge) {
        this.itemID = itemID;
        this.maxAge = maxAge;
    }

    public String getItemID() {
        return this.itemID;
    }

    public void setItemID(final String itemID) {
        this.itemID = itemID;
    }

    public int getMaxAge() {
        return this.maxAge;
    }

    public void setMaxAge(final int maxAge) {
        this.maxAge = maxAge;
    }

}