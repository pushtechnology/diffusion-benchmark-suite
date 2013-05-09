package util;

import java.io.Closeable;

/**
 * A factory interface...
 * @author nitsanw
 *
 * @param <T>
 */
public interface Factory<T> extends Closeable {
    
    /**
     * @return a new T instance.
     */
    T create();
}
