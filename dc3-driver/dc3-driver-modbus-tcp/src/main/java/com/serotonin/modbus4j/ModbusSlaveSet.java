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

import com.serotonin.modbus4j.exception.ModbusInitException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Abstract ModbusSlaveSet class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class ModbusSlaveSet extends Modbus {

    private LinkedHashMap<Integer, ProcessImage> processImages = new LinkedHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * <p>addProcessImage.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     */
    public void addProcessImage(ProcessImage processImage) {
        lock.writeLock().lock();
        try {
            processImages.put(processImage.getSlaveId(), processImage);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * <p>removeProcessImage.</p>
     *
     * @param slaveId a int.
     * @return a boolean.
     */
    public boolean removeProcessImage(int slaveId) {
        lock.writeLock().lock();
        try {
            return (processImages.remove(slaveId) != null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * <p>removeProcessImage.</p>
     *
     * @param processImage a {@link ProcessImage} object.
     * @return a boolean.
     */
    public boolean removeProcessImage(ProcessImage processImage) {
        lock.writeLock().lock();
        try {
            return (processImages.remove(processImage.getSlaveId()) != null);
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * <p>getProcessImage.</p>
     *
     * @param slaveId a int.
     * @return a {@link ProcessImage} object.
     */
    public ProcessImage getProcessImage(int slaveId) {
        lock.readLock().lock();
        try {
            return processImages.get(slaveId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a copy of the current process images
     *
     * @return a {@link Collection} object.
     */
    public Collection<ProcessImage> getProcessImages() {
        lock.readLock().lock();
        try {
            return new HashSet<>(processImages.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Starts the slave. If an exception is not thrown, this method does not return, but uses the thread to execute the
     * listening.
     *
     * @throws ModbusInitException if necessary
     */
    abstract public void start() throws ModbusInitException;

    /**
     * <p>stop.</p>
     */
    abstract public void stop();
}
