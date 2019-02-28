package mcgui;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * TCP implementation of BasicCommunicator
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class TCPCommunicator implements BasicCommunicator {
    
    Multicaster mc;
    
    ArrayList<ObjectOutputStream> connections;
    TreeSet<Integer> deadset;
    
    public TCPCommunicator() {
        connections = new ArrayList<ObjectOutputStream>();
        deadset = new TreeSet<Integer>();
    }

    void setMulticaster(Multicaster mc) {
        this.mc = mc;
    }
    
    void connect(int myid,String host,int port) {
        try {
            Socket s = new Socket(host, port);
            ObjectOutputStream w = new ObjectOutputStream(s.getOutputStream());
            connections.add(w);
            w.writeInt(myid);
            w.flush();
        } catch(Exception e) {
            System.err.println("Exception in TCPListener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send a message to a certain receiver. Transmitted over TCP and
     * guarantees reliable FIFO point to point communication.
     */
    public void basicsend(int receiver,Message message) {
        //System.out.println("basicsend("+receiver+","+message+")");
        synchronized(mcgui.Lock.lock) {
            try {
                ObjectOutputStream out = connections.get(receiver);
                out.writeObject(message);
                out.flush();
            } catch(IOException e) {
                peerdown(receiver);
            }
            //System.out.println("end basicsend("+receiver+","+message+")");
        }
    }

    /**
     * Send message to connection
     */
    void peerdown(int peer) {
        synchronized(mcgui.Lock.lock) {
            // System.out.println("Starting PeerDownThread("+peer+")");
            if(!deadset.contains(peer)) {
                deadset.add(peer);
                new Thread(new PeerDownThread(this,mc,peer)).start();
            }
        }
    }
    
    /**
     * 
     */
    void receive(int peer,Message message) {
        synchronized(mcgui.Lock.lock) {
            if(deadset.contains(peer)) {
                return;
            }
            //System.out.println("receive("+peer+","+message+")");
            mc.basicreceive(peer,message);
        }
    }

    private class PeerDownThread implements Runnable {

        /** 
         * The time to wait before all forwarded messages from a
         * crashed process are assumed to have been received
         */
        public static final int CRASHWAITTIME = 5000;
        
        TCPCommunicator com;
        Multicaster mc;
        int peer;
        
        public PeerDownThread(TCPCommunicator com, Multicaster mc, int peer) {
            this.com = com;
            this.mc = mc;
            this.peer = peer;
        }
        
        public void run() {
            try {
                Thread.sleep(CRASHWAITTIME);
            } catch(InterruptedException e) {
                // No interrupt calls are made, so nothing to do here
            }
            synchronized(mcgui.Lock.lock) {
                mc.basicpeerdown(peer);
            }
        }
    }
    
}
