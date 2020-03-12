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
package com.github.s7connector.api.factory;

import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.S7Serializer;
import com.github.s7connector.impl.serializer.S7SerializerImpl;

/**
 * S7 Serializer factory
 * 
 * @author Thomas Rudin
 *
 */
public class S7SerializerFactory {

	/**
	 * Builds a new serializer with given connector
	 * 
	 * @param connector
	 *            the connector to use
	 * @return a serializer instance
	 */
	public static S7Serializer buildSerializer(final S7Connector connector) {
		return new S7SerializerImpl(connector);
	}

}
