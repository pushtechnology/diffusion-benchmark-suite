package util;

import java.lang.reflect.Field;
//CHECKSTYLE:OFF
import sun.misc.Unsafe;
//CHECKSTYLE:ON

/**
 * When you want to play with fire...
 * 
 * @author nitsanw
 * 
 */
public final class UnsafeAccess {
    /** I'm not here. */
    private UnsafeAccess() {

    }

    /** The unsafe instance. */
    public static final Unsafe UNSAFE;
    static {
        try {
            // This is a bit of voodoo to force the unsafe object into
            // visibility and acquire it.
            // This is not playing nice, but as an established back door it is
            // not likely to be
            // taken away.
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
