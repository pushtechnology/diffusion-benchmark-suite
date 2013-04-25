package clients;

import com.pushtechnology.diffusion.api.ServerConnectionListener;

/**
 * Extending the connection listener interface to support full connect options.
 * 
 * @author nitsanw
 *
 */
public interface ExperimentClient extends ServerConnectionListener {
    /**
     * @return the topics to be subsribed to on connection
     */
    String[] getInitialTopics();
}
