/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.serotonin.modbus4j;

import com.serotonin.modbus4j.base.KeyedModbusLocator;
import com.serotonin.modbus4j.base.ReadFunctionGroup;
import com.serotonin.modbus4j.base.SlaveAndRange;
import com.serotonin.modbus4j.locator.BaseLocator;

import java.util.*;

/**
 * A class for defining the information required to obtain in a batch.
 * <p>
 * The generic parameterization represents the class of the key that will be used to find the results in the BatchRead
 * object. Typically String would be used, but any Object is valid.
 * <p>
 * Some modbus devices have non-contiguous sets of values within a single register range. These gaps between values may
 * cause the device to return error responses if a request attempts to read them. In spite of this, because it is
 * generally more efficient to read a set of values with a single request, the batch read by default will assume that no
 * such error responses will be returned. If your batch request results in such errors, it is recommended that you
 * separate the offending request to a separate batch read object, or you can use the "contiguous requests" setting
 * which causes requests to be partitioned into only contiguous sets.
 *
 * @param <K> - Type of read
 * @author mlohbihler
 * @version 5.0.0
 */
public class BatchRead<K> {
    private final List<KeyedModbusLocator<K>> requestValues = new ArrayList<>();

    /**
     * See documentation above.
     */
    private boolean contiguousRequests = false;

    /**
     * If this value is false, any error response received will cause an exception to be thrown, and the entire batch to
     * be aborted (unless exceptionsInResults is true - see below). If set to true, error responses will be set as the
     * result of all affected locators and the entire batch will be attempted with no such exceptions thrown.
     */
    private boolean errorsInResults = false;

    /**
     * If this value is false, any exceptions thrown will cause the entire batch to be aborted. If set to true, the
     * exception will be set as the result of all affected locators and the entire batch will be attempted with no such
     * exceptions thrown.
     */
    private boolean exceptionsInResults = false;

    /**
     * A batch may be split into an arbitrary number of individual Modbus requests, and so a given batch may take
     * an arbitrary amount of time to complete. The cancel field is provided to allow the batch to be cancelled.
     */
    private boolean cancel;

    /**
     * This is what the data looks like after partitioning.
     */
    private List<ReadFunctionGroup<K>> functionGroups;

    /**
     * <p>isContiguousRequests.</p>
     *
     * @return a boolean.
     */
    public boolean isContiguousRequests() {
        return contiguousRequests;
    }

    /**
     * <p>Setter for the field <code>contiguousRequests</code>.</p>
     *
     * @param contiguousRequests a boolean.
     */
    public void setContiguousRequests(boolean contiguousRequests) {
        this.contiguousRequests = contiguousRequests;
        functionGroups = null;
    }

    /**
     * <p>isErrorsInResults.</p>
     *
     * @return a boolean.
     */
    public boolean isErrorsInResults() {
        return errorsInResults;
    }

    /**
     * <p>Setter for the field <code>errorsInResults</code>.</p>
     *
     * @param errorsInResults a boolean.
     */
    public void setErrorsInResults(boolean errorsInResults) {
        this.errorsInResults = errorsInResults;
    }

    /**
     * <p>isExceptionsInResults.</p>
     *
     * @return a boolean.
     */
    public boolean isExceptionsInResults() {
        return exceptionsInResults;
    }

    /**
     * <p>Setter for the field <code>exceptionsInResults</code>.</p>
     *
     * @param exceptionsInResults a boolean.
     */
    public void setExceptionsInResults(boolean exceptionsInResults) {
        this.exceptionsInResults = exceptionsInResults;
    }

    /**
     * <p>getReadFunctionGroups.</p>
     *
     * @param master a {@link ModbusMaster} object.
     * @return a {@link List} object.
     */
    public List<ReadFunctionGroup<K>> getReadFunctionGroups(ModbusMaster master) {
        if (functionGroups == null)
            doPartition(master);
        return functionGroups;
    }

    /**
     * <p>addLocator.</p>
     *
     * @param id      a K object.
     * @param locator a {@link BaseLocator} object.
     */
    public void addLocator(K id, BaseLocator<?> locator) {
        addLocator(new KeyedModbusLocator<>(id, locator));
    }

    private void addLocator(KeyedModbusLocator<K> locator) {
        requestValues.add(locator);
        functionGroups = null;
    }

    /**
     * <p>isCancel.</p>
     *
     * @return a boolean.
     */
    public boolean isCancel() {
        return cancel;
    }

    /**
     * <p>Setter for the field <code>cancel</code>.</p>
     *
     * @param cancel a boolean.
     */
    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    //
    //
    // Private stuff
    //
    private void doPartition(ModbusMaster master) {
        Map<SlaveAndRange, List<KeyedModbusLocator<K>>> slaveRangeBatch = new HashMap<>();

        // Separate the batch into slave ids and read functions.
        List<KeyedModbusLocator<K>> functionList;
        for (KeyedModbusLocator<K> locator : requestValues) {
            // Find the function list for this slave and range. Create it if necessary.
            functionList = slaveRangeBatch.get(locator.getSlaveAndRange());
            if (functionList == null) {
                functionList = new ArrayList<>();
                slaveRangeBatch.put(locator.getSlaveAndRange(), functionList);
            }

            // Add this locator to the function list.
            functionList.add(locator);
        }

        // Now that we have locators grouped into slave and function, check each read function group and break into
        // parts as necessary.
        Collection<List<KeyedModbusLocator<K>>> functionLocatorLists = slaveRangeBatch.values();
        FunctionLocatorComparator comparator = new FunctionLocatorComparator();
        functionGroups = new ArrayList<>();
        for (List<KeyedModbusLocator<K>> functionLocatorList : functionLocatorLists) {
            // Sort the list by offset.
            Collections.sort(functionLocatorList, comparator);

            // Break into parts by excessive request length. Remember the max item count that we can ask for, for
            // this function
            int maxReadCount = master.getMaxReadCount(functionLocatorList.get(0).getSlaveAndRange().getRange());

            // Create the request groups.
            createRequestGroups(functionGroups, functionLocatorList, maxReadCount);
            //System.out.println("requests: " + functionGroups.size());
        }
    }

    /**
     * We aren't trying to do anything fancy here, like some kind of artificial optimal group for performance or
     * anything. We pretty much just try to fit as many locators as possible into a single valid request, and then move
     * on.
     * <p>
     * This method assumes the locators have already been sorted by start offset.
     */
    private void createRequestGroups(List<ReadFunctionGroup<K>> functionGroups, List<KeyedModbusLocator<K>> locators,
                                     int maxCount) {
        ReadFunctionGroup<K> functionGroup;
        KeyedModbusLocator<K> locator;
        int index;
        int endOffset;
        // Loop for creation of groups.
        while (locators.size() > 0) {
            functionGroup = new ReadFunctionGroup<>(locators.remove(0));
            functionGroups.add(functionGroup);
            endOffset = functionGroup.getStartOffset() + maxCount - 1;

            // Loop for adding locators to the current group
            index = 0;
            while (locators.size() > index) {
                locator = locators.get(index);
                boolean added = false;

                if (locator.getEndOffset() <= endOffset) {
                    if (contiguousRequests) {
                        // The locator must at least abut the other locators in the group.
                        if (locator.getOffset() <= functionGroup.getEndOffset() + 1) {
                            functionGroup.add(locators.remove(index));
                            added = true;
                        }
                    } else {
                        functionGroup.add(locators.remove(index));
                        added = true;
                    }
                }

                if (!added) {
                    // This locator does not fit inside the current function...
                    if (locator.getOffset() > endOffset)
                        // ... and since the list is sorted by offset, no other locators can either, so quit the loop.
                        break;

                    // ... but there still may be other locators that can, so increment the index
                    index++;
                }
            }
        }
    }

    class FunctionLocatorComparator implements Comparator<KeyedModbusLocator<K>> {
        @Override
        public int compare(KeyedModbusLocator<K> ml1, KeyedModbusLocator<K> ml2) {
            return ml1.getOffset() - ml2.getOffset();
        }
    }
}
