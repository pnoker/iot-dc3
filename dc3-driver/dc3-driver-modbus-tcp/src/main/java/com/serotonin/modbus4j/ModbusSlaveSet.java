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
 * @version 2025.6.0
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
     * Starts the slave. If an exception is not thrown, this method doesn't return, but uses the thread to execute the
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
