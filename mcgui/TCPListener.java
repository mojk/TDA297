package mcgui;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Helper class for TCPCommunicator. A Thread that listens to one socket.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class TCPListener extends Thread {

    TCPCommunicator communicator;
    Socket socket;
    
    public TCPListener(TCPCommunicator communicator, Socket socket) {
        this.communicator = communicator;
        this.socket = socket;
    }
    
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            int number = in.readInt();
            try {
                while(true) {
                    Message message = (Message)in.readObject();
                    communicator.receive(number, message);
                }
            } catch(java.io.EOFException e) {
                communicator.peerdown(number);
            }
        } catch (Exception e) {
            System.err.println("Exception in TCPListener: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
