package com.github.pnoker.driver.bean;

import com.github.s7connector.api.S7Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PointVariable {
    private int dbNum;
    private int byteOffset;
    private int bitOffset;
    private int size;
    private S7Type type;
    private Class<?> fieldType;

    public PointVariable(int dbNum, int byteOffset, int bitOffset, int size, String s7Type) {
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
