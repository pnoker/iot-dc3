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
