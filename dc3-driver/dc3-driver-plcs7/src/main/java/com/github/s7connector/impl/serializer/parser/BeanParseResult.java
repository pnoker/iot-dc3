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

import java.util.Vector;

public final class BeanParseResult {

    /**
     * The needed blocksize
     */
    public int blockSize;

    /**
     * The Bean entries
     */
    public Vector<BeanEntry> entries = new Vector<BeanEntry>();

}
