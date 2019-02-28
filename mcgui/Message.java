package mcgui;

import java.io.Serializable;

/**
 * Message class to be extended.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public abstract class Message implements Serializable {

    /**
     * The sender of the message
     */
    protected int sender;
    
    public Message(int sender) {
        this.sender = sender;
    }
    
    /**
     * Returns the sender of the message
     */
    public int getSender() {
        return sender;
    }
    
    /**
     * Forbidden constructor so that sender must be initialized.
     */
    private Message() {
        ;
    }

}