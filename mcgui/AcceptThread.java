package mcgui;

import java.io.*;
import java.net.*;

/**
 * Helper class for TCPCommunicator. A Thread that listens for connections and starts TCPListeners.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class AcceptThread extends Thread {
    TCPCommunicator communicator;
    int portnumber;
    
    public AcceptThread(TCPCommunicator communicator,int portnumber) {
        this.communicator = communicator;
        this.portnumber = portnumber;
    }
    
    public void run() {
        try {
            ServerSocket server = new ServerSocket(portnumber);
            while (true) {
                Socket s = server.accept();
                TCPListener l = new TCPListener(communicator, s);
                l.start();
            }
        } catch (Exception e) {
            System.err.println("Exception in AcceptThread: " + e.getMessage());
            e.printStackTrace();
        }
    }




}

