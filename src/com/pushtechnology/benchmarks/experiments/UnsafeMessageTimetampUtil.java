/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.experiments;

import java.nio.ByteBuffer;


import com.pushtechnology.benchmarks.util.UnsafeAccess;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DataMessageImpl;
import com.pushtechnology.diffusion.message.MessageImpl;

/**
 * A hack, put in place to bypass message creation in the RC benchmark.
 * 
 * @author nitsanw
 *
 */
public final class UnsafeMessageTimetampUtil {
    /** memory offset to support unsafe access to message BB. */
    private static final long BB_OFFSET;
    /** memory offset to support unsafe access to message header length. */
    private static final long HEADER_LEN_OFFSET;

    /** Can't touch this. */
    private UnsafeMessageTimetampUtil() {
    }
    static {
        try {
            BB_OFFSET = UnsafeAccess.UNSAFE
                    .objectFieldOffset(DataMessageImpl.class
                            .getDeclaredField("theByteBuffer"));
            HEADER_LEN_OFFSET = UnsafeAccess.UNSAFE
                    .objectFieldOffset(MessageImpl.class
                            .getDeclaredField("theHeaderLength"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Steal the header length so we know where data starts.
     * 
     * @param m ...
     * @return the header length, or data position
     */
    public static int getHeaderLength(TopicMessage m) {
        return UnsafeAccess.UNSAFE.getInt(m, HEADER_LEN_OFFSET);
    }
    
    /**
     * Steal the message ByteBuffer.
     * 
     * @param m ...
     * @return the ByteBuffer
     */
    public static ByteBuffer getBuffer(TopicMessage m) {
        return (ByteBuffer) UnsafeAccess.UNSAFE.getObject(m, BB_OFFSET);
    }
    
    /**
     * Directly write the time value into the message.
     * 
     * @param m ...
     * @param time ...
     */
    public static void insertTimsetamp(TopicMessage m, long time) {
        getBuffer(m).putLong(getHeaderLength(m), time);
    }
    
    /**
     * Directly write the time value into the message.
     * 
     * @param m ...
     * @return the timestamp
     */
    public static long getTimsetamp(TopicMessage m) {
        return getBuffer(m).getLong(getHeaderLength(m));
    }
}
