/*
Copyright 2016 S7connector members (github.com/s7connector)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.s7connector.impl.serializer.parser;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.s7connector.api.S7Serializable;
import com.github.s7connector.api.annotation.S7Variable;
import com.github.s7connector.api.S7Type;

public final class BeanParser {

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
