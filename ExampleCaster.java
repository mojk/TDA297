
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

    int msg_id = 0; //unique message id for every message that is sent by an node
    int leader; //Variable to decide which one is the leader
    int seq_number = 0; // Sequence number that the leader sends out
    int leader_seq = 0; // The leader has to have an extra sequence number
    int[] participants; // Current alive participants in the network
    int turn; // Process local turn variable
    int[] vc;
    int[] sent;
    ExampleMessage confirmation_msg;
    ExampleMessage deliver_msg;
    ExampleMessage final_msg;
    ExampleMessage stored_msg;

    /* These three queues are used for undelivered msgs */
    TreeMap<Integer,ExampleMessage> msgs_0 = new TreeMap<>();
    TreeMap<Integer,ExampleMessage> msgs_1 = new TreeMap<>(); 
    TreeMap<Integer,ExampleMessage> msgs_2 = new TreeMap<>();
    HashMap<Integer, TreeMap<Integer,ExampleMessage>> msg_bag = initHashMap(msgs_0, msgs_1, msgs_2);
    HashMap<Integer, TreeMap<Integer,ExampleMessage>> leader_bag = initHashMap(msgs_0, msgs_1, msgs_2);



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
        sent = new int[hosts]; //initializing the sent-vector that the leader uses
        for(int i = 0; i < vc.length; i++) {
            vc[i] = 0;
        }
        for(int i = 0; i < hosts; i++) {
            participants[i] = i;
        }
        
        leader_election(participants);
    }

    public int leader_election(int[] nodes) {
        mcui.debug("Time to decide who is the leader");
        int[] p = new int[nodes];
        for(int i=0; i < nodes; i++) {
            p[i] = i;
        }
        int new_leader = getMinValue(p);
        mcui.debug(leader + " is the new leader!");   
        return new_leader;
    }

    /* Function to fetch the lowest value of an array, used for leader election */
    public static int getMinValue(int[] a) {
        int minValue = a[0];
        for(int i = 0; i < a.length; i++) {
            if(a[0] < minValue)
                minValue = a[0];
        }
        return minValue;
    }

    /**
     * The GUI calls this module to multicast a message
     * We start by increasing the message id
     * We then send it to the assigned leader
     */

    public void cast(String messagetext) {
     if(id == leader) {
        ExampleMessage leader_msg = new ExampleMessage(id, messagetext, msg_id, leader_seq, false);
        leader_msg.confirm = true;
        mcui.debug("Leader wants to broadcast, but must be accepted by himself first..!");
        bcom.basicsend(leader,leader_msg);
        msg_id++;
        mcui.debug("Now waiting for response from myself...");    
        mcui.debug("Leaders local sequence number is .. " + leader_seq);    
        mcui.debug("Global sequence number is.. " + seq_number);

    } else {
        ExampleMessage normal_msg = new ExampleMessage(id, messagetext, msg_id, seq_number, false);
        mcui.debug("Requesting to broadcast... sending message " + normal_msg.msg_id + " to the leader");
        normal_msg.confirm = true;
        bcom.basicsend(leader,normal_msg);
        msg_id++;
        mcui.debug("Now waiting for response from the leader...");    
        mcui.debug("I'm at sequence number.. " + seq_number);    
        mcui.debug("Global sequence number is.. " + seq_number);
}
}
    /**
     * Receive a basic message
     * @param message  The message received
     */

    public void basicreceive(int peer, Message message) {
    //----------------------------------------------------------------
    // Leader Execution
    // Check for confirmation messages
    // Check if recieved message is ready for delivering
    // Leader confirms messages that are arriving
    // Iterate to check if we can broadcast messages
    //
    //----------------------------------------------------------------
    if(id == leader) {
        ExampleMessage new_msg = (ExampleMessage) message;
        
        /* Confirmation Phase */

        if(new_msg.msg_id == sent[peer] && new_msg.confirm == true) {

            /* If you recieve a request from yourself */
            if(peer == leader) {
                sent[peer]++; // indicate that a node has requested to broadcast a message
                confirmation_msg = new ExampleMessage(id, new_msg.text, new_msg.msg_id, seq_number, true); //confirmation message
                mcui.debug("(Leader) Request for message " + new_msg.msg_id + " from " + peer + " seq_number=0 " + seq_number);
                seq_number++; //Increasing the global sequence-number
                mcui.debug("Increasing the global sequence number.. => " + seq_number);
                bcom.basicsend(leader,confirmation_msg);

            /* For the rest of the network */

            } else if (peer != leader) {
                sent[peer]++; // indicate that a node has requested to broadcast a message
                confirmation_msg = new ExampleMessage(id, new_msg.text, new_msg.msg_id, seq_number, true); //confirmation message
                mcui.debug("Leader just recieved a message " + new_msg.msg_id + " from " + peer + " assigning it a sequence number... " + seq_number);
                seq_number++; //Increasing the global sequence-number
                mcui.debug("Increasing the global sequence number.. => " + seq_number);
                bcom.basicsend(peer,confirmation_msg);
        } 
    }
 
         /* If we've recieved a confirmation message of a message that we can send in the future, we store it */

        if (peer == leader && new_msg.seq_number > leader_seq && new_msg.confirm == true && new_msg.msg_id > vc[id]) {
            mcui.debug("Message with sequence number " + new_msg.seq_number + " confirmed with id " + new_msg.msg_id+" but i should not send it now!.. Storing in bag..");
            storeMsg(new_msg, id);

            /* If the leader has recieved a confirm message from himself thats is valid */

        } else if (peer == leader && new_msg.msg_id == vc[id] && new_msg.seq_number == leader_seq && new_msg.confirm == true) {
                final_msg = new ExampleMessage(id, new_msg.text, vc[id], leader_seq, false);
                mcui.debug("Request accepted! Broadcasting message " + final_msg.msg_id + " to everyone..");
                mcui.debug("The sequence number on this message is " + final_msg.seq_number);
                leader_seq++;
                mcui.debug("Increasing my local (leader) sequence number.. => " + leader_seq);
                for(int i=0; i < hosts; i++) {
                    /* Sends to everyone except itself */
                    if(i != id) {
                        bcom.basicsend(i,final_msg);
                    }
                }
                vc[id]++;
                mcui.deliver(id, final_msg.text, "from myself!");
        }

        /* If you've recieved a message that is ready to be delivered */

        if (new_msg.seq_number == leader_seq && new_msg.confirm == false && new_msg.msg_id == vc[peer]) {         
            deliver_msg = new ExampleMessage(peer, new_msg.text, vc[peer], leader_seq, false);
            vc[peer]++;
            mcui.debug( id + " message " + deliver_msg.msg_id + " recieved from " + peer + ".. delivering it..");
            mcui.deliver(peer, deliver_msg.text);
            leader_seq++;
            mcui.debug("Increasing my local (leader) sequence number.. => " + leader_seq);
        }
//-------------------------------------------------------------------------------------------- NEW STUFF

            /* We iterate through our message bag to see if we have any messages
             that are approved with a sequence number that we can start to broadcast */

            mcui.debug("Leader: Lets see if i have any messages i can broadcast..");
            TreeMap<Integer,ExampleMessage> list = msg_bag.get(id);
            TreeMap<Integer,ExampleMessage> list_copy = new TreeMap<>(list);
            Iterator it = list_copy.values().iterator();
            while(it.hasNext()) {
                ExampleMessage m = (ExampleMessage) it.next();
                mcui.debug("Pulled out a message from, it has the seq_number.. " +  m.seq_number +" and the id.. " + m.msg_id);
                mcui.debug("My sequence number is at.." + leader_seq +" and my vectorclock is at.. " +vc[id]);
                if (m.msg_id == vc[id]+1 && m.seq_number == (leader_seq+1)) {
                    stored_msg = new ExampleMessage(id, m.text, (vc[id]+1), (leader_seq+1), false);
                    mcui.debug("Fetched a message from our bag.. broadcasting message " + m.msg_id + " to everyone..");
                    mcui.debug("The sequence number on this message is " + m.seq_number);
                    leader_seq++;
                    mcui.debug("Increasing my local sequence number.. => " + leader_seq);    

                    for(int i=0; i < hosts; i++) {
                        /* Sends to everyone except itself */
                        if(i != id) {
                            bcom.basicsend(i,stored_msg);
                        }
                    }
                    vc[id]++;
                    mcui.deliver(id, stored_msg.text, "from myself!");
                    removeMsg(id);

                    } else if (m.msg_id == vc[id] && m.seq_number == leader_seq) {
                    stored_msg = new ExampleMessage(id, m.text, vc[id], leader_seq, false);
                    mcui.debug("Fetched a message from our bag.. broadcasting message " + m.msg_id + " to everyone..");
                    mcui.debug("The sequence number on this message is " + m.seq_number);
                    leader_seq++;
                    mcui.debug("Increasing my local sequence number.. => " + leader_seq);    

                    for(int i=0; i < hosts; i++) {
                        /* Sends to everyone except itself */
                        if(i != id) {
                            bcom.basicsend(i,stored_msg);
                        }
                    }
                    vc[id]++;
                    mcui.deliver(id, stored_msg.text, "from myself!");
                    removeMsg(id);

                    }
            }
            
            mcui.debug("Latest message recieved was.. from " + new_msg.getSender() + " Sequence id was " + new_msg.seq_number);
            if(new_msg.seq_number == leader_seq+1) {
                mcui.debug("Did we miss something?");
                mcui.debug("My seq_number is at => " + leader_seq + " and new_msg has the seq_num" + new_msg.seq_number);

            }
            //Implement store delivery queue
//-------------------------------------------------------------------------------------------- END NEW STUFF

    //----------------------------------------------------------------
    // Normal Node execution
    // Check for confirmation messages
    // Check if the recieved message is supposed to be delivered
    // Iterate through our mesage_bag to see if we can broadcast anything (meaning global_seq == message_seq)
    //----------------------------------------------------------------

    } else {
        ExampleMessage new_msg = (ExampleMessage) message;

            /* If we've recieved a confirmation message of a message that we shall send in the future, we store it */

            if (peer == leader && new_msg.seq_number > seq_number && new_msg.confirm == true ) {
                mcui.debug("Message with sequence number " + new_msg.seq_number + " confirmed from "+ new_msg.getSender() +"! But i should not send it now!.. Storing in bag..");
                storeMsg(new_msg, id);

            /* Normal node recieved a confirmation message from the leader */

        }   else if ( peer == leader && new_msg.msg_id == vc[id] && new_msg.seq_number == seq_number && new_msg.confirm == true) {
                final_msg = new ExampleMessage(id,new_msg.text,vc[id],seq_number, false);

                mcui.debug("Request accepted! Broadcasting message " + final_msg.msg_id + " to everyone..");
                mcui.debug("The sequence number on this message is " + final_msg.seq_number);

                seq_number++;
                mcui.debug("Increasing my local sequence number.. => " + seq_number);    

                for(int i=0; i < hosts; i++) {
                    /* Sends to everyone except itself */
                    if(i != id) {
                        bcom.basicsend(i,final_msg);
                    }
                }
                vc[id]++;
                mcui.deliver(id, final_msg.text, "from myself!");

//-------------------------------------------------------------------------------------------- NEW STUFF
        /* Recieved a message that is ready to be delivered form someone else */

        }
            if(new_msg.seq_number == seq_number && new_msg.confirm == false && new_msg.msg_id == vc[peer]) {
                deliver_msg = new ExampleMessage(peer, new_msg.text, vc[peer], seq_number, false);
                vc[peer]++;
                mcui.debug( id + " message " + deliver_msg.msg_id + " recieved from " + peer + ".. delivering it..");
                mcui.deliver(peer, deliver_msg.text);
                seq_number++;
                mcui.debug("Increasing my local sequence number.. => " + seq_number);
            }

            /* We iterate through our message bag to see if we have any messages we can start to broadcsat */

            mcui.debug("Lets see if i have any messages i can broadcast..");
            TreeMap<Integer,ExampleMessage> list = msg_bag.get(id);
            TreeMap<Integer,ExampleMessage> list_copy = new TreeMap<>(list);
            Iterator it = list_copy.values().iterator();
            while(it.hasNext()) {

                ExampleMessage m = (ExampleMessage) it.next();
                mcui.debug("Pulled out a message from , it has the seq_number.. " +  m.seq_number +" and the id.. " + m.msg_id);
                mcui.debug("My sequence number is at.." + seq_number +" and my vectorclock is at.. " +vc[id]);
                if (m.msg_id == vc[id]+1 && m.seq_number == (seq_number+1)) {
                    stored_msg = new ExampleMessage(id, m.text, (vc[id]+1), (seq_number+1), false);
                    mcui.debug("Fetched a message from our bag.. broadcasting message " + m.msg_id + " to everyone..");
                    mcui.debug("The sequence number on this message is " + m.seq_number);
                    seq_number++;
                    mcui.debug("Increasing my local sequence number.. => " + seq_number);    

                    for(int i=0; i < hosts; i++) {
                        /* Sends to everyone except itself */
                        if(i != id) {
                            bcom.basicsend(i,stored_msg);
                        }
                    }
                    vc[id]++;
                    mcui.deliver(id, stored_msg.text, "from myself!");
                    removeMsg(id);
                    } else if(m.msg_id == vc[id] && m.seq_number == seq_number) {
                    stored_msg = new ExampleMessage(id, m.text, m.msg_id, m.seq_number, false);
                    mcui.debug("Fetched a message from our bag.. broadcasting message " + m.msg_id + " to everyone..");
                    mcui.debug("The sequence number on this message is " + m.seq_number);
                    seq_number++;
                    mcui.debug("Increasing my local sequence number.. => " + seq_number);    

                    for(int i=0; i < hosts; i++) {
                        /* Sends to everyone except itself */
                        if(i != id) {
                            bcom.basicsend(i,stored_msg);
                        }
                    }
                    vc[id]++;
                    mcui.deliver(id, stored_msg.text, "from myself!");
                    removeMsg(id);

                    }
            }
            mcui.debug("Latest message recieved was.. from " + new_msg.getSender() + " Sequence id was " + new_msg.seq_number);
//-------------------------------------------------------------------------------------------- END NEW STUFF

    }
}
    public void LeaderstoreMsg(ExampleMessage msg, int sender) {
        TreeMap<Integer,ExampleMessage> msg_list = leader_bag.get(sender);
        if( !msg_list.containsValue(msg) ) {
            msg_list.put(msg.msg_id, msg);
        }
    }

    public void storeMsg(ExampleMessage msg, int sender) {
        TreeMap<Integer,ExampleMessage> msg_list = msg_bag.get(sender);
        if( !msg_list.containsValue(msg) ) {
            msg_list.put(msg.msg_id, msg);
        }
    }

    /* Method for removing a delivered message from your bag 
    /*
    /* Here we will fetch the TreeMap for the respective sender
    /* Since we sort the messages by their msg_id, we will always have the lowest id in the first position
    /* And we only remove a message if it is the next one in line, which will always be placed first in the TreeMap
    */
    public void LeaderremoveMsg(int sender) {
        TreeMap<Integer,ExampleMessage> msg_list = leader_bag.get(sender);
        Entry<Integer,ExampleMessage> entry = msg_list.firstEntry();
        msg_list.remove(entry.getKey());
    }

    public void removeMsg(int sender) {
        TreeMap<Integer,ExampleMessage> msg_list = msg_bag.get(sender);
        Entry<Integer,ExampleMessage> entry = msg_list.firstEntry();
        msg_list.remove(entry.getKey());
    }

    /**
     * Signals that a peer is down and has been down for a while to
     * allow for messages taking different paths from this peer to
     * arrive.
     * @param peer	The dead peer
     */

    public void basicpeerdown(int peer) {
        mcui.debug("Peer "+peer+" has been dead for a while now!");
        participants--;
        leader_election(participants);
    }
}
