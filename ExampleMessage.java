
import mcgui.*;

/**
 * Message implementation for ExampleCaster.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class ExampleMessage extends Message implements Comparable<ExampleMessage> {
        
    String text;
    int msg_id;
    boolean confirm = false;
    int seq_number;
        
    public ExampleMessage(int sender,String text,int msg_id, int seq_number, boolean confirm) {
        super(sender);
        this.text = text;
        this.msg_id = msg_id;
        this.seq_number = seq_number;
        this.confirm = confirm;
    }

    @Override
    public int compareTo(ExampleMessage msg) {
        //int compare_peer = msg.getPeer();
        int compare_seq_number = msg.getSeq();
        if( (this.seq_number - compare_seq_number) == 0 )
            return 1;
        return 0;
    }
    
    /**
     * Returns the text of the message only. The toString method can
     * be implemented to show additional things useful for debugging
     * purposes.
     */
    public String getText() {
        return text;
    }
    public int getNum() {
        return msg_id;
    }
    public int getSeq() {
        return seq_number;
    }
    
    public static final long serialVersionUID = 0;
}
