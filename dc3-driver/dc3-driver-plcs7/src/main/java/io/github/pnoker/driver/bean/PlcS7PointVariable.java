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

package io.github.pnoker.driver.bean;

import io.github.pnoker.driver.api.S7Type;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
public class PlcS7PointVariable {
    private int dbNum;
    private int byteOffset;
    private int bitOffset;
    private int size;
    private S7Type type;
    private Class<?> fieldType;

    public PlcS7PointVariable(int dbNum, int byteOffset, int bitOffset, int size, String s7Type) {
        this.dbNum = dbNum;
        this.byteOffset = byteOffset;
        this.bitOffset = bitOffset;
        this.size = size;
        getS7TypeAndType(s7Type);

    }

    private void getS7TypeAndType(String s7Type) {
        switch (s7Type) {
            case "bool":
                this.type = S7Type.BOOL;
                this.fieldType = Boolean.class;
                break;
            case "byte":
                this.type = S7Type.BYTE;
                this.fieldType = Byte.class;
                break;
            case "int":
                this.type = S7Type.INT;
                this.fieldType = Short.class;
                break;
            case "dint":
                this.type = S7Type.DINT;
                this.fieldType = Long.class;
                break;
            case "word":
                this.type = S7Type.WORD;
                this.fieldType = Integer.class;
                break;
            case "dword":
                this.type = S7Type.DWORD;
                this.fieldType = Long.class;
                break;
            case "real":
                this.type = S7Type.REAL;
                this.fieldType = Float.class;
                break;
            case "date":
                this.type = S7Type.DATE;
                this.fieldType = Date.class;
                break;
            case "time":
                this.type = S7Type.TIME;
                this.fieldType = Long.class;
                break;
            case "datetime":
                this.type = S7Type.DATE_AND_TIME;
                this.fieldType = Long.class;
                break;
            default:
                this.type = S7Type.STRING;
                this.fieldType = Boolean.class;
                break;
        }
    }
}
