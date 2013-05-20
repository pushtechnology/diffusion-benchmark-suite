package experiments;

import java.nio.ByteBuffer;

import util.UnsafeAccess;

import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.message.DataMessageImpl;
import com.pushtechnology.diffusion.message.MessageImpl;

public class EvilMessageAccessHack {
    /** memory offset to support unsafe access to message BB. */
    private static final long BB_OFFSET;
    /** memory offset to support unsafe access to message header length. */
    private static final long HEADER_LEN_OFFSET;

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
    public static int getHeaderLength(TopicMessage m){
        return UnsafeAccess.UNSAFE.getInt(m, HEADER_LEN_OFFSET);
    }
    public static ByteBuffer getBuffer(TopicMessage m){
        return (ByteBuffer) UnsafeAccess.UNSAFE.getObject(m, BB_OFFSET);
    }
}
