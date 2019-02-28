package mcgui;

import java.io.Serializable;

/**
 * Class to extend when creating a multicast module
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public abstract class Multicaster {

    /**
     * Id of the client.
     */
    protected int id;

    /**
     * Initial number of hosts in network.
     */
    protected int hosts;

    /**
     * The user interface.
     */
    protected mcgui.MulticasterUI mcui;

    /**
     * The communication backend.
     */
    protected mcgui.BasicCommunicator bcom;

    /**
     * Returns id of the client.
     */
    public int getId() {
        return id;
    }
    
    /* ========== Abstract methods to implement ========== */

    /**
     * Method for initialization of things.
     */
    public abstract void init();

    /**
     * Multicast a message
     * @param message  The message to be multicasted
     */
    public abstract void cast(String messagetext);
    
    /**
     * Receive a basic message
     * @param message  The message received
     */
    public abstract void basicreceive(int peer,Message message);

    /**
     * Signals that a peer is down and has been down for a while to
     * allow for messages taking different paths from this peer to
     * arrive.
     * @param peer	The dead peer
     */
    public abstract void basicpeerdown(int peer);


    /* ========== Not important for extending classes ========== */

    /**
     * Set the id of the client
     * @param id	The id
     */
    public void setId(int id, int hosts) {
        this.id = id;
        this.hosts = hosts;
    }

    /**
     * Set the UI that should be used
     * @param mcui	The UI
     */
    public void setUI(MulticasterUI mcui) {
        this.mcui = mcui;
    }

    /**
     * Set BasicCommunicator that should be used
     * @param bcom	The Basic communicator
     */
    public void setCommunicator(BasicCommunicator bcom) {
        this.bcom = bcom;
    }

    /**
     * Enables the user to start using the system.
     */
    public void enableUI() {
        mcui.enableSending();
    }

}


