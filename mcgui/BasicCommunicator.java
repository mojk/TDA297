package mcgui;

import java.io.Serializable;

/**
 * Interface to use when creating a multicast module for sending messages over the network
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public interface BasicCommunicator {
    
    /**
     * Send a message to a certain receiver.
     */
    public void basicsend(int receiver,Message message);

}
