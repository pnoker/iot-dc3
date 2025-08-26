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

package com.serotonin.modbus4j.sero.util;


/**
 * <p>Abstract ProgressiveTask class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class ProgressiveTask implements Runnable {
    protected boolean completed = false;
    private boolean cancelled = false;
    private ProgressiveTaskListener listener;

    /**
     * <p>Constructor for ProgressiveTask.</p>
     */
    public ProgressiveTask() {
        // no op
    }

    /**
     * <p>Constructor for ProgressiveTask.</p>
     *
     * @param l a {@link ProgressiveTaskListener} object.
     */
    public ProgressiveTask(ProgressiveTaskListener l) {
        listener = l;
    }

    /**
     * <p>cancel.</p>
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * <p>isCancelled.</p>
     *
     * @return a boolean.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * <p>isCompleted.</p>
     *
     * @return a boolean.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * <p>run.</p>
     */
    public final void run() {
        while (true) {
            if (isCancelled()) {
                declareFinished(true);
                break;
            }

            runImpl();

            if (isCompleted()) {
                declareFinished(false);
                break;
            }
        }
        completed = true;
    }

    /**
     * <p>declareProgress.</p>
     *
     * @param progress a float.
     */
    protected void declareProgress(float progress) {
        ProgressiveTaskListener l = listener;
        if (l != null)
            l.progressUpdate(progress);
    }

    private void declareFinished(boolean cancelled) {
        ProgressiveTaskListener l = listener;
        if (l != null) {
            if (cancelled)
                l.taskCancelled();
            else
                l.taskCompleted();
        }
    }

    /**
     * Implementers of this method MUST return from it occasionally so that the cancelled status can be checked. Each
     * return must leave the class and thread state with the expectation that runImpl will not be called again, while
     * acknowledging the possibility that it will.
     * <p>
     * Implementations SHOULD call the declareProgress method with each runImpl execution such that the listener can be
     * notified.
     * <p>
     * Implementations MUST set the completed field to true when the task is finished.
     */
    abstract protected void runImpl();
}
