package util;


/**
 * A factory interface...
 * @author nitsanw
 *
 * @param <T>
 */
public interface Factory<T> {
    
    /**
     * @return a new T instance.
     */
    T create();
    
    /**
     * cleanup. 
     */
    void close();
}
