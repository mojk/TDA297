package mcgui;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Starter class that starts up a Multicaster module together with a
 * GUI and the TCPCmmunicator.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class Main {

    /**
     * Send message to connection
     */
    public static void main(String args[]) {
        String classname = "";
        int id = -1;
        int myport = -1;
        int numberofclients = 0;
        ArrayList<String[]> setup = null;
        TCPCommunicator communicator = null;

        try {
            /* Setting up according to arguments. 
             *
             *This is where you get the needed network information to set up
             * communications, instead of pulling names from a list.
             */
            classname = args[0];
            id=Integer.parseInt(args[1]);
            setup = SetupParser.parseFile(args[2]);
            numberofclients = Integer.parseInt(setup.get(0)[0]);
            String me[] = setup.get(id+1);
            myport = Integer.parseInt(me[1]);
            /* Dealing with errors*/
        } catch(Exception e) {
            if(e instanceof java.io.IOException) {
                System.err.println("Error reading setup file: "+e.getMessage());
                e.printStackTrace();
            } else {
                System.err.println("Error - Incorrect setup: "+e);
                System.err.println("");
                System.err.println("Usage: java Main <classname> <ID> <setupfile>");
                System.err.println("   where: <ID> is a non-negative integer");
                System.err.println("          <setupfile> is a file containing the network setup");
            }
            System.exit(1);
        }

        try {
            /* Starting listening */
            communicator = new TCPCommunicator();
            AcceptThread at = new AcceptThread(communicator,myport);
            at.start();
            
            /* Waining for startup of others*/
            Thread.sleep(2000);
            
            /* Connecting*/
            for(int i=0; i < numberofclients; i++) {
                String client[] = setup.get(i+1);
                communicator.connect(id,client[0], Integer.parseInt(client[1]));
            }
        } catch(Exception e) {
            System.err.println("Trouble connecting: "+e.getMessage());
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
            
        Multicaster mc = null;
        try {
            Class implclass = Main.class.getClassLoader().loadClass(classname);
            mc = (Multicaster)implclass.newInstance();
            communicator.setMulticaster(mc);
        } catch(Exception e) {
            System.err.println("Error loading class: "+e.getMessage());
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
        
        /* Starting the GUI */
        mcgui.GUI.startGUI(mc,id);
        mc.setId(id, numberofclients);
        mc.setCommunicator(communicator);
        mc.init();
        //communicator.setMulticaster();
        
        /* No communications to set up in this one so we enable the GUI */
        mc.enableUI();
        
    }


}
        
        
        
        
        