import mcgui.*;
import java.util.*;
import java.util.Map.Entry;
import java.lang.*;

/**
 * Reliable Broadcast with Total & Casual Ordering
 * @author Mikael Gordani
 */

public class ExampleCaster extends Multicaster {

    int msg_id = 0;
    int leader;
    int seq_number = 0;
    int leader_seq = 0;
    int vc[];
    int requests[];

    ArrayList<Integer> participants;
    ExampleMessage bc_msg;
    ExampleMessage stored_msg;
    ExampleMessage ack_msg;
    ExampleMessage deliver_msg;
    ExampleMessage un_deliver_msg;

    /* TreeMap for storing messages and the second one is for the leader to store "unconfirmed" messages */
    TreeMap<Integer, ExampleMessage> messages;
    TreeMap<Integer, ExampleMessage> unconfirmed_messages;
    HashMap<Integer, TreeMap<Integer,ExampleMessage>> msg_bag;
    HashMap<Integer, TreeMap<Integer,ExampleMessage>> leader_bag;

    public void re_init_leader() {
        msg_bag = new HashMap<>(); //clear every message bag
        leader_bag = new HashMap<>(); //leader clears its leader bag
        seq_number = 0; // reset global sequence numbe
        leader_seq = 0; // leader also must reset his local sequene number
        requests = new int[participants.size()]; //clear the requests

        for(int i = 0; i < participants.size(); i++) {
            messages = new TreeMap<>();
            unconfirmed_messages = new TreeMap<>();
            msg_bag.put(participants.get(i), messages);
            leader_bag.put(participants.get(i), unconfirmed_messages);
        }
        leader = leader_election(participants);
    }

    public void init() {
        mcui.debug("The network has "+hosts+" hosts!");

        participants = new ArrayList<>();
        msg_bag = new HashMap<>();
        leader_bag = new HashMap<>();

        for(int i = 0; i < hosts; i++) {
            messages = new TreeMap<>();
            unconfirmed_messages = new TreeMap<>();
            msg_bag.put(i, messages);
            leader_bag.put(i, unconfirmed_messages);
            participants.add(i);
            mcui.debug("Adding... " + i);
        }

        vc = new int[participants.size()]; // Initializing the vector clock
        requests = new int[participants.size()]; // initializing the sent-vector that the leader uses
        leader = leader_election(participants); // Electing leader 
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

    /* Broadcast for leader, sends to everyone including himself */
    public void leaderBroadcast(ExampleMessage msg) {
        for(int i=0; i < participants.size(); i++) {
            bcom.basicsend(participants.get(i),msg);
        }
        if(msg.ack == true)
            mcui.debug("Confirming that this message is ready to be delivered!: from " + msg.origin);         
        mcui.debug("Broadcasted out: \""+msg.text+"\"");
    }

    /* Simple broadcast where you send to everyone expect yourself */
    public void broadcast(ExampleMessage msg) {
        for(int i=0; i < participants.size(); i++) {
            /* Sends to everyone except itself */
            if(i != id) {
                bcom.basicsend(participants.get(i),msg);
            }
        }
        mcui.debug("Broadcasted out: \""+msg.text+"\"");
    }

    /* Depending if you are the leader or not, you will use two different types of broadcasts */
    public void cast(String messagetext) {
        if(id == leader) {
            bc_msg = new ExampleMessage(id, messagetext, msg_id, leader_seq, false, id);
            leaderBroadcast(bc_msg); //bc to everyone
            storeMsg(bc_msg, id, msg_bag); //save it to your bag
            msg_id++; //increase your msg_id
        } else {
            bc_msg = new ExampleMessage(id, messagetext, msg_id, seq_number, false, id);
            broadcast(bc_msg); //bc to everyone except yourself
            storeMsg(bc_msg, id, msg_bag); //save it to your bag
            msg_id++; //increase your msg_id
        }
    }
    
    /**
     * Receive a basic message
     * @param message  The message received
     */

    public void basicreceive(int peer,Message message) {

    //----------------------------------------------------------------
    // Leader code
    //----------------------------------------------------------------
        if(id == leader) {
            deliver_msg  = (ExampleMessage) message;
            un_deliver_msg = (ExampleMessage) message;
            ack_msg = (ExampleMessage) message;

            /* Recieving a request */
            if(ack_msg.ack == false && ack_msg.msg_id == requests[peer]) {
                mcui.debug("Receved a request from " + ack_msg.getSender() + " seq_number = " + ack_msg.seq_number);
                ack_msg.seq_number = seq_number;
                if(ack_msg.origin != leader)
                    storeMsg(ack_msg, peer, msg_bag); //save to bag
                ack_msg.ack = true;
                requests[peer]++;
                seq_number++;
                leaderBroadcast(ack_msg); //bc to everyone to say which message in the next one to be deliverd

                /* Recieving a request from the future */
            } else if (ack_msg.ack == false && ack_msg.msg_id > requests[peer]) {
                mcui.debug("Receved a request from " + ack_msg.getSender() + " seq_number = " + ack_msg.seq_number);
                mcui.debug("Global seq_number is " + seq_number + " stashing it" );
                storeMsg(ack_msg, peer, leader_bag);

            }
            /* Check if we have received any messages that we now can confirm */
            FetchFromBagAndConfirm();

            /* We check if the message that we've recieved is ready to be delivered, otherwise we store it in our bag and check if we have a message that is ready to be delivered */
            if(deliver_msg.ack == true && deliver_msg.msg_id == vc[deliver_msg.origin] && deliver_msg.seq_number == leader_seq) {
                mcui.debug("Receved a deliverable from " + deliver_msg.origin + " seq_number = " + deliver_msg.seq_number);
                if(deliver_msg.origin == id) {
                    mcui.deliver(id, deliver_msg.text, "from myself!");
                    leader_seq++;
                    vc[deliver_msg.origin]++;
                } else if(deliver_msg.origin != id) {
                    mcui.deliver(deliver_msg.origin, deliver_msg.text);
                    leader_seq++;
                    vc[deliver_msg.origin]++;
                }
                
                /* Check if we have any confirmed messages we can deliver */
                if(isInBag(un_deliver_msg))
                    FetchFromBagAndDeliver();

        } // end leader-code

    //----------------------------------------------------------------
    // Non-leader code
    //----------------------------------------------------------------

    } else {
        deliver_msg = (ExampleMessage) message;
        un_deliver_msg = (ExampleMessage) message;
        mcui.debug("Just received a message, let's check its properties");
        mcui.debug("Sender= " + deliver_msg.origin + " status= " + deliver_msg.ack);
        /* We check if the message that we've recieved is ready to be delivered, otherwise we store it in our bag and check if we have a message that is ready to be delivered */
        if(deliver_msg.ack == true && deliver_msg.msg_id == vc[deliver_msg.origin] && deliver_msg.seq_number == seq_number) {
            mcui.debug("Receved a deliverable from " + deliver_msg.origin + " seq_number = " + deliver_msg.seq_number);
            if(deliver_msg.origin == id) {
                mcui.deliver(id, deliver_msg.text, "from myself!");
                seq_number++;
                vc[deliver_msg.origin]++;
            } else if( deliver_msg.origin != id) {
                mcui.deliver(deliver_msg.origin, deliver_msg.text);
                seq_number++;
                vc[deliver_msg.origin]++;
            } else {
                storeMsg(deliver_msg, deliver_msg.origin, msg_bag);
            }
            /* Check if we have any confirmed messages we can deliver */
            if(isInBag(un_deliver_msg))
                FetchFromBagAndDeliver();
        }
        } // end non-leader code
    }
    public boolean isInBag(ExampleMessage undel_msg) {
        mcui.debug("Checking bag to see if we have recieved the message before..");
        for(int i = 0; i < participants.size(); i++) {
            TreeMap<Integer,ExampleMessage> list = msg_bag.get(i);
            TreeMap<Integer,ExampleMessage> list_copy = new TreeMap<>(list);
            Iterator it = list_copy.values().iterator();
            while(it.hasNext()) {
                ExampleMessage m = (ExampleMessage) it.next();
                mcui.debug("Comparing the message, it has the seq_number.. " +  undel_msg.seq_number +" and the id.. " + undel_msg.msg_id + " its ack is " + undel_msg.ack);
                mcui.debug("Pulled out a message, it has the seq_number.. " +  m.seq_number +" and the id.. " + m.msg_id + " its ack is " + m.ack);
                mcui.debug("My sequence number is at.." + seq_number +" and my vectorclock is at.. " +vc[i]);

                if (m.origin == undel_msg.origin && m.msg_id == undel_msg.msg_id && m.seq_number == undel_msg.seq_number && m.ack != undel_msg.ack) {
                    return true;
                }
            }
        }
        return false;
    }

    public void FetchFromBagAndConfirm() {
        mcui.debug("Let's see if I've recieved something that I now can confirm!");
        for(int i = 0; i < participants.size(); i++) {
            TreeMap<Integer,ExampleMessage> list = leader_bag.get(i);
            TreeMap<Integer,ExampleMessage> list_copy = new TreeMap<>(list);
            Iterator it = list_copy.values().iterator();
            while(it.hasNext()) {
                ExampleMessage m = (ExampleMessage) it.next();
                if(m.msg_id == requests[m.getSender()]) {
                    ack_msg.seq_number = seq_number;
                    storeMsg(ack_msg, ack_msg.origin, msg_bag); //save to bag
                    ack_msg.ack = true;
                    requests[m.getSender()]++;
                    seq_number++;
                    leaderBroadcast(ack_msg);
                    removeMsg(i, leader_bag); //remove message
                }
            }
        }
    }
    public void FetchFromBagAndDeliver() {
        mcui.debug("Lets see if i have any messages i can deliver..");
        for(int i = 0; i < participants.size(); i++) {
            TreeMap<Integer,ExampleMessage> list = msg_bag.get(i);
            TreeMap<Integer,ExampleMessage> list_copy = new TreeMap<>(list);
            Iterator it = list_copy.values().iterator();
            while(it.hasNext()) {
                ExampleMessage m = (ExampleMessage) it.next();
                mcui.debug("Pulled out a message from , it has the seq_number.. " +  m.seq_number +" and the id.. " + m.msg_id);
                if(id == leader)
                    mcui.debug("My sequence number is at.." + leader_seq +" and my vectorclock is at.. " +vc[i]);
                mcui.debug("My sequence number is at.." + seq_number +" and my vectorclock is at.. " +vc[i]);
                if (m.msg_id == vc[i]+1 && m.seq_number == leader_seq) {
                    mcui.debug("Fetched a message from our bag.. broadcasting message " + m.msg_id + " to everyone..");
                    mcui.debug("The sequence number on this message is " + m.seq_number);
                    if(id == leader)
                        leader_seq++;
                    seq_number++;
                    if(id == leader)
                        mcui.debug("Increasing my local sequence number.. => " + leader_seq);
                    mcui.debug("Increasing my local sequence number.. => " + seq_number);      
                    vc[i]++;
                    if(i == id) {
                        mcui.deliver(i, m.text, "from myself!");
                    } else {
                        mcui.deliver(i, m.text);
                    }
                    removeMsg(i, msg_bag);
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

    public void basicpeerdown(int peer) { //TODO FIX SO EVERY NODE CLEARS THEIR MESSAGEBAG AND LEADER ANNOUNCES NEW SEQUENCENUMBER
        mcui.debug("Peer "+peer+" has been dead for a while now!");
        for(int i = 0; i < participants.size(); i++) {
            if(peer == participants.get(i))
                participants.remove(i);
        }
        for(int i = 0; i < participants.size(); i++) {
            mcui.debug("Remaining guys are... " + participants.get(i));
        }
        Collections.sort(participants);
        if(peer == leader)
            re_init_leader();
    }

    /* Function for storing a message in the msg_bag */
    public void storeMsg(ExampleMessage msg, int sender, HashMap<Integer, TreeMap<Integer,ExampleMessage>> bag) {
        TreeMap<Integer,ExampleMessage> msg_list = bag.get(sender);
        if( !msg_list.containsValue(msg) ) {
            msg_list.put(msg.msg_id, msg);
        }
    }

    /* Function for removing a message from the msg_bag */
    public void removeMsg(int sender, HashMap<Integer, TreeMap<Integer,ExampleMessage>> bag) {
        TreeMap<Integer,ExampleMessage> msg_list = bag.get(sender);
        Entry<Integer,ExampleMessage> entry = msg_list.firstEntry();
        msg_list.remove(entry.getKey());
    }
}
