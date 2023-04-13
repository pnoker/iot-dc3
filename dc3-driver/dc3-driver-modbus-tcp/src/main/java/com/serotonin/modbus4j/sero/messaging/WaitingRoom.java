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

package com.serotonin.modbus4j.sero.messaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The waiting room is a place for request messages to hang out while awaiting their responses.
 *
 * @author Matthew Lohbihler
 */
class WaitingRoom {
    private static final Log LOG = LogFactory.getLog(WaitingRoom.class);

    private final Map<WaitingRoomKey, Member> waitHere = new HashMap<WaitingRoomKey, Member>();

    private WaitingRoomKeyFactory keyFactory;

    void setKeyFactory(WaitingRoomKeyFactory keyFactory) {
        this.keyFactory = keyFactory;
    }

    /**
     * The request message should be sent AFTER entering the waiting room so that the (vanishingly small) chance of a
     * response being returned before the thread is waiting for it is eliminated.
     *
     * @return
     */
    void enter(WaitingRoomKey key) {
        Member member = new Member();
        synchronized (this) {
            while (waitHere.get(key) != null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Duplicate waiting room key found. Waiting for member to leave.");
                try {
                    wait();
                } catch (InterruptedException e) {
                    // no op
                }
            }
            //            Member dup = waitHere.get(key);
            //            if (dup != null) {
            //                
            //                throw new WaitingRoomException("Waiting room too crowded. Already contains the key " + key);
            //            }

            waitHere.put(key, member);
        }
    }

    IncomingResponseMessage getResponse(WaitingRoomKey key, long timeout) throws WaitingRoomException {
        // Get the member.
        Member member;
        synchronized (this) {
            member = waitHere.get(key);
        }

        if (member == null)
            throw new WaitingRoomException("No member for key " + key);

        // Wait for the response.
        return member.getResponse(timeout);
    }

    void leave(WaitingRoomKey key) {
        // Leave the waiting room
        synchronized (this) {
            waitHere.remove(key);

            // Notify any threads that are waiting to get in. This could probably be just a notify() call.
            notifyAll();
        }
    }

    /**
     * This method is used by the data listening thread to post responses as they are received from the transport.
     *
     * @param response the response message
     * @throws WaitingRoomException
     */
    void response(IncomingResponseMessage response) throws WaitingRoomException {
        WaitingRoomKey key = keyFactory.createWaitingRoomKey(response);
        if (key == null)
            // The key factory can return a null key if the response should be ignored. 
            return;

        Member member;

        synchronized (this) {
            member = waitHere.get(key);
        }

        if (member != null)
            member.setResponse(response);
        else
            throw new WaitingRoomException("No recipient was found waiting for response for key " + key);
    }

    /**
     * This class is used by network message controllers to manage the blocking of threads sending confirmed messages.
     * The instance itself serves as a monitor upon which the sending thread can wait (with a timeout). When a response
     * is received, the message controller can set it in here, automatically notifying the sending thread that the
     * response is available.
     *
     * @author Matthew Lohbihler
     */
    class Member {
        private IncomingResponseMessage response;

        synchronized void setResponse(IncomingResponseMessage response) {
            this.response = response;
            notify();
        }

        synchronized IncomingResponseMessage getResponse(long timeout) {
            // Check if there is a response object now.
            if (response != null)
                return response;

            // If not, wait the timeout and then check again.
            waitNoThrow(timeout);
            return response;
        }

        private void waitNoThrow(long timeout) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
