package com.github.pnoker.driver.bp;

import com.github.s7connector.api.S7Serializable;
import com.github.s7connector.api.S7Type;
import com.github.s7connector.api.annotation.S7Variable;
import com.github.s7connector.impl.serializer.parser.BeanEntry;
import com.github.s7connector.impl.serializer.parser.BeanParseResult;
import com.github.s7connector.impl.serializer.parser.BeanParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author pnoker
 */
public class Bp {
    /**
     * Local Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(BeanParser.class);

    /**
     * Returns the wrapper for the primitive type
     *
     * @param primitiveType
     * @return
     */
    private static Class<?> getWrapperForPrimitiveType(final Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return Boolean.class;
        } else if (primitiveType == byte.class) {
            return Byte.class;
        } else if (primitiveType == int.class) {
            return Integer.class;
        } else if (primitiveType == float.class) {
            return Float.class;
        } else if (primitiveType == double.class) {
            return Double.class;
        } else if (primitiveType == long.class) {
            return Long.class;
        } else {
            // Fallback
            return primitiveType;
        }
    }

    /**
     * Parses a Class
     *
     * @param jclass
     * @return
     * @throws Exception
     */
    public static BeanParseResult parse(final Class<?> jclass) throws Exception {
        final BeanParseResult res = new BeanParseResult();
        logger.trace("Parsing: " + jclass.getName());

        for (final Field field : jclass.getFields()) {
            final S7Variable dataAnnotation = field.getAnnotation(S7Variable.class);

            if (dataAnnotation != null) {
                logger.trace("Parsing field: " + field.getName());
                logger.trace("		type: " + dataAnnotation.type());
                logger.trace("		byteOffset: " + dataAnnotation.byteOffset());
                logger.trace("		bitOffset: " + dataAnnotation.bitOffset());
                logger.trace("		size: " + dataAnnotation.size());
                logger.trace("		arraySize: " + dataAnnotation.arraySize());

                final int offset = dataAnnotation.byteOffset();

                // update max offset
                if (offset > res.blockSize) {
                    res.blockSize = offset;
                }

                if (dataAnnotation.type() == S7Type.STRUCT) {
                    // recurse
                    logger.trace("Recursing...");
                    final BeanParseResult subResult = parse(field.getType());
                    res.blockSize += subResult.blockSize;
                    logger.trace("	New blocksize: " + res.blockSize);
                }

                logger.trace("	New blocksize (+offset): " + res.blockSize);

                // Add dynamic size
                res.blockSize += dataAnnotation.size();

                // Plain element
                final BeanEntry entry = new BeanEntry();
                entry.byteOffset = dataAnnotation.byteOffset();
                entry.bitOffset = dataAnnotation.bitOffset();
                entry.field = field;
                entry.type = getWrapperForPrimitiveType(field.getType());
                entry.size = dataAnnotation.size();
                entry.s7type = dataAnnotation.type();
                entry.isArray = field.getType().isArray();
                entry.arraySize = dataAnnotation.arraySize();

                if (entry.isArray) {
                    entry.type = getWrapperForPrimitiveType(entry.type.getComponentType());
                }

                // Create new serializer
                final S7Serializable s = entry.s7type.getSerializer().newInstance();
                entry.serializer = s;

                res.blockSize += (s.getSizeInBytes() * dataAnnotation.arraySize());
                logger.trace("	New blocksize (+array): " + res.blockSize);

                if (s.getSizeInBits() > 0) {
                    boolean offsetOfBitAlreadyKnown = false;
                    for (final BeanEntry parsedEntry : res.entries) {
                        if (parsedEntry.byteOffset == entry.byteOffset) {
                            offsetOfBitAlreadyKnown = true;
                        }
                    }
                    if (!offsetOfBitAlreadyKnown) {
                        res.blockSize++;
                    }
                }

                res.entries.add(entry);
            }
        }

        logger.trace("Parsing done, overall size: " + res.blockSize);

        return res;
    }

    /**
     * Parses an Object
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public static BeanParseResult parse(final Object obj) throws Exception {
        return parse(obj.getClass());
    }

}
