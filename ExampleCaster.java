import mcgui.*;
import java.util.*;
import java.util.Map.Entry;
import java.lang.*;

/**
 * Simple example of how to use the Multicaster interface.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class ExampleCaster extends Multicaster {

    int msg_id = 0;
    int leader;
    int seq_number = 0;
    int leader_seq = 0;
    boolean[] ack = new boolean[3];
    int vc[];
    int requests[];
    ArrayList<Integer> participants;
    ExampleMessage bc_msg;
    ExampleMessage stored_msg;
    ExampleMessage ack_msg;


    /* These three queues are used for undelivered msgs */
    TreeMap<Integer,ExampleMessage> msgs_0 = new TreeMap<>();
    TreeMap<Integer,ExampleMessage> msgs_1 = new TreeMap<>(); 
    TreeMap<Integer,ExampleMessage> msgs_2 = new TreeMap<>();
    HashMap<Integer, TreeMap<Integer,ExampleMessage>> msg_bag = initHashMap(msgs_0, msgs_1, msgs_2);

    /* Function to initalize the TreeMap */

    public static HashMap<Integer, TreeMap<Integer,ExampleMessage>> initHashMap(TreeMap<Integer,ExampleMessage> list0, TreeMap<Integer,ExampleMessage> list1, TreeMap<Integer,ExampleMessage> list2) {
        HashMap<Integer, TreeMap<Integer,ExampleMessage>> mb = new HashMap<>();
        mb.put(0,list0);
        mb.put(1,list1);
        mb.put(2,list2);
        return mb;
    }

    public void init() {
        mcui.debug("The network has "+hosts+" hosts!");
        vc = new int[hosts]; // Initializing the vector clock
        requests = new int[hosts]; //initializing the sent-vector that the leader uses
        participants = new ArrayList<>();

        for(int i = 0; i < hosts; i++) {
            vc[i] = 0;
            requests[i] = 0;
            participants.add(i);
        }
        leader = leader_election(participants);
    }

    /* Leader Election Function */

    public int leader_election(ArrayList<Integer> nodes) {
        mcui.debug("Time to decide who is the leader");
        int min = nodes.get(0);
        for (int i : nodes){
            min = min < i ? min : i;
        }
        mcui.debug(min + " is the new leader!");   
        return min;
    }
        
    /**
     * If you are the leader, you must add the leaders local sequence-number to the message
     * if not, you just add the normal sequence number
     * Nevertheless, before you send the message, you acknowledge that you've recieved it
     * and store it in your message bag until you're sure that everyone else has recieved it
     * and update the acknowledgement-vector accordingly
     * once it has reached true on every entry, and its sequence number matches your own
     * it will be time to deliver the message
     */

    public void cast(String messagetext) {
        if(id == leader) {
            bc_msg = new ExampleMessage(id, messagetext, msg_id, leader_seq, false);

            for(int i=0; i < hosts; i++) {
                /* Sends to everyone except itself */
                if(i != id) {
                    bcom.basicsend(i,bc_msg);
                }
            }
            mcui.debug("Sent out: \""+messagetext+"\"");
            msg_id++;
            storeMsg(bc_msg,id);    

        } else {
            bc_msg = new ExampleMessage(id, messagetext, msg_id, seq_number, false);

            for(int i=0; i < hosts; i++) {
                /* Sends to everyone except itself */
                if(i != id) {
                    bcom.basicsend(i,bc_msg);
                }
            }
            mcui.debug("Sent out: \""+messagetext+"\"");
            msg_id++;
            storeMsg(bc_msg,id);     
        }
    }
    
    /**
     * Receive a basic message
     * @param message  The message received
     */
    public void basicreceive(int peer,Message message) {
        /* Code for leader */
        if(id == leader) {
            /* Recieving a request from someone else than yourself */
            if(peer != id) {
            
            /* Recieving a request from someone else yourself */
            } else {

            }

            FetchFromBagAndDeliver();
        /* Code for non-leader nodes */
        } else {
            FetchFromBagAndDeliver();
    }
}
    public void FetchFromBagAndDeliver() {
            mcui.debug("Lets see if i have any messages i can broadcast..");
            for(int i = 0; i < participants.size(); i++) {
                TreeMap<Integer,ExampleMessage> list = msg_bag.get(i);
                TreeMap<Integer,ExampleMessage> list_copy = new TreeMap<>(list);
                Iterator it = list_copy.values().iterator();
                while(it.hasNext()) {

                    ExampleMessage m = (ExampleMessage) it.next();
                    mcui.debug("Pulled out a message from , it has the seq_number.. " +  m.seq_number +" and the id.. " + m.msg_id);
                    mcui.debug("My sequence number is at.." + seq_number +" and my vectorclock is at.. " +vc[i]);
                    if (m.msg_id == vc[i]+1 && m.seq_number == seq_number) {
                        stored_msg = new ExampleMessage(i, m.text, vc[i], seq_number, false);
                        mcui.debug("Fetched a message from our bag.. broadcasting message " + m.msg_id + " to everyone..");
                        mcui.debug("The sequence number on this message is " + m.seq_number);
                        seq_number++;
                        mcui.debug("Increasing my local sequence number.. => " + seq_number);    
                        vc[i]++;
                        mcui.deliver(i, stored_msg.text, "from myself!");
                        removeMsg(i);
                }
            }
        }
    }

    /**
     * Signals that a peer is down and has been down for a while to
     * allow for messages taking different paths from this peer to
     * arrive.
     * @param peer  The dead peer
     */
    public void basicpeerdown(int peer) {
        mcui.debug("Peer "+peer+" has been dead for a while now!");
        participants.remove(peer);
        leader = leader_election(participants);
    }

    /* Function for storing a message in the msg_bag */

    public void storeMsg(ExampleMessage msg, int sender) {
        TreeMap<Integer,ExampleMessage> msg_list = msg_bag.get(sender);
        if( !msg_list.containsValue(msg) ) {
            msg_list.put(msg.msg_id, msg);
        }
    }

    /* function for removing a message from the msg_bag */

    public void removeMsg(int sender) {
        TreeMap<Integer,ExampleMessage> msg_list = msg_bag.get(sender);
        Entry<Integer,ExampleMessage> entry = msg_list.firstEntry();
        msg_list.remove(entry.getKey());
    }
}
