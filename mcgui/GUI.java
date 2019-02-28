package mcgui;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * A graphical interface that uses a provided Multicaster module for
 * multicasting messages to itself and other clients using a supplied
 * Multicaster module.
 * 
 * The interface supports multicasting messages, and stress testing
 * the system by sending a specified number of messages at a specific
 * time (or right away if no time is given).
 * 
 * The interface shows the most recent messages delivered to it and
 * the most recent debug messages.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class GUI extends JFrame implements MulticasterUI {
    
    private final int        defaultStress = 10;
    
    private final int        maxMessages = 200;
    private final int        maxDebugs   = 1000;
    
    java.util.LinkedList<String> recentMessages;
    java.util.LinkedList<String> recentDebugs;

    /* Whether old messages or debug messages have been discarded */
    private boolean deliverMaxReached = false;
    private boolean debugMaxReached   = false;

    /* Synchronization flags for message and debug graphics updates */
    private boolean displayUpdating   = false;
    private boolean displayNeedUpdate = false;

  
    private JPanel     sendPanel;

    private JLabel     messageLabel;     
    private JTextField messageField;
    private JButton    sendButton;

    private JLabel     stressNumberLabel;     
    private JTextField stressNumber;
    private JLabel     stressTimeLabel;     
    private JTextField stressTime;
    private JButton    stressButton;

    private JPanel      showPanel;

    private JTextArea   receiveArea;
    private JScrollPane receiveScr;
  
    private JTextArea   debugArea;
    private JScrollPane debugScr;


    private JPanel    interactPanel;

  
    private Multicaster mc;
    private int name;
    
    private PrintWriter deliverout = null;

    private final int pad = 5;
  
    private GUI(Multicaster mc, int name)
    {
        this.mc = mc;
        this.name = name;
        setup();
    
        recentMessages = new java.util.LinkedList<String>();
        recentDebugs   = new java.util.LinkedList<String>();

        String deliverfile = "deliveredby"+name+".txt";
        try{
            deliverout = new PrintWriter(new FileWriter(deliverfile));
        } catch(IOException e) {
            System.err.println("Cannot open file "+deliverfile+": "+e);
            System.err.println("System will run but will not provide this logging.");
            deliverout = null;
        }
    }
  
    private void setup()
    {
        setTitle("MC - "+name);

    
        /* ---------------------------------------- */
        SpringLayout sendLayout = new SpringLayout();

        sendPanel = new JPanel();
        sendPanel.setLayout(sendLayout);
        messageField = new JTextField(40);
    
        sendButton =  new JButton("Cast");
        sendButton.setEnabled(false);

        messageLabel = new JLabel("Message");

        messageField.addKeyListener(new java.awt.event.KeyListener() {
                public void keyPressed(java.awt.event.KeyEvent e){
                    ;
                }
	
                public void keyReleased(java.awt.event.KeyEvent e) {
                    ;
                }

                public void keyTyped(java.awt.event.KeyEvent e) {
                    if(e.getKeyChar() == java.awt.event.KeyEvent.VK_ENTER && sendButton.isEnabled()) {
                        multicastMessage();
                    }
                }
            });
    
        sendButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    multicastMessage();
                }
            });

        /* ---------- */


        stressNumberLabel = new JLabel("#");
        stressNumber = new JTextField(3);
        stressNumber.setText(""+defaultStress);

        stressTimeLabel = new JLabel("Time");
        stressTime = new JTextField(6);

        stressButton = new JButton("Stress");
        stressButton.setEnabled(false);


        stressTime.addKeyListener(new java.awt.event.KeyListener() {
                public void keyPressed(java.awt.event.KeyEvent e){
                    ;
                }
	
                public void keyReleased(java.awt.event.KeyEvent e) {
                    ;
                }

                public void keyTyped(java.awt.event.KeyEvent e) {
                    if(e.getKeyChar() == java.awt.event.KeyEvent.VK_ENTER && stressButton.isEnabled()) {
                        setStress();
                    }
                }
            });
    
        stressButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setStress();
                }
            });

        /* ---------- */

        sendPanel.add(messageField);
        sendPanel.add(sendButton);
        sendPanel.add(stressNumber);
        sendPanel.add(stressTime);
        sendPanel.add(stressButton);

        sendPanel.add(messageLabel);
        sendPanel.add(stressNumberLabel);
        sendPanel.add(stressTimeLabel);
        int labelpad = messageLabel.getMinimumSize().height + 2*pad;

        sendLayout.putConstraint(SpringLayout.NORTH, messageField,    labelpad, SpringLayout.NORTH, sendPanel);
        sendLayout.putConstraint(SpringLayout.NORTH, sendButton,      labelpad, SpringLayout.NORTH, sendPanel);
        sendLayout.putConstraint(SpringLayout.NORTH, stressNumber,    labelpad, SpringLayout.NORTH, sendPanel);
        sendLayout.putConstraint(SpringLayout.NORTH, stressTime,      labelpad, SpringLayout.NORTH, sendPanel);
        sendLayout.putConstraint(SpringLayout.NORTH, stressButton,    labelpad, SpringLayout.NORTH, sendPanel);

        sendLayout.putConstraint(SpringLayout.WEST,  messageField,    pad, SpringLayout.WEST,  sendPanel);
        sendLayout.putConstraint(SpringLayout.EAST,  stressButton,   -pad, SpringLayout.EAST,  sendPanel);
        sendLayout.putConstraint(SpringLayout.EAST,  stressTime,     -pad, SpringLayout.WEST,  stressButton);
        sendLayout.putConstraint(SpringLayout.EAST,  stressNumber,   -pad, SpringLayout.WEST,  stressTime);
        sendLayout.putConstraint(SpringLayout.EAST,  sendButton,     -pad, SpringLayout.WEST,  stressNumber);
        sendLayout.putConstraint(SpringLayout.EAST,  messageField,   -pad, SpringLayout.WEST,  sendButton);

        sendLayout.putConstraint(SpringLayout.SOUTH, messageLabel,      -pad, SpringLayout.NORTH, messageField);
        sendLayout.putConstraint(SpringLayout.SOUTH, stressNumberLabel, -pad, SpringLayout.NORTH, stressNumber);
        sendLayout.putConstraint(SpringLayout.SOUTH, stressTimeLabel,   -pad, SpringLayout.NORTH, stressTime);

        sendLayout.putConstraint(SpringLayout.WEST, messageLabel,      0, SpringLayout.WEST, messageField);
        sendLayout.putConstraint(SpringLayout.WEST, stressNumberLabel, 0, SpringLayout.WEST, stressNumber);
        sendLayout.putConstraint(SpringLayout.WEST, stressTimeLabel,   0, SpringLayout.WEST, stressTime);


        Component sendC = sendPanel;
        /* ---------------------------------------- */

        GridLayout showLayout = new GridLayout(0,1,pad,pad);
        showPanel = new JPanel();
        showPanel.setLayout(showLayout);

        receiveArea  = new JTextArea("", 5, 10);
        receiveArea.setEditable(false);
        receiveScr   = new JScrollPane(receiveArea);
        receiveScr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        debugArea  = new JTextArea("", 5, 10);
        debugArea.setEditable(false);
        debugScr   = new JScrollPane(debugArea);
        debugScr.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    
        showPanel.add(receiveScr);
        showPanel.add(debugScr);

        Component showC = showPanel;

        /* ---------------------------------------- */
    
        SpringLayout slayout = new SpringLayout();
        java.awt.LayoutManager layout = slayout;
        getContentPane().setLayout(layout);
        add(sendPanel);
        add(showPanel);
    

        int bmin = sendButton.getMinimumSize().height+labelpad;
        int tmin = messageField.getMinimumSize().height;
        final int sendH = 2*pad + (bmin < tmin ? tmin : bmin);

        slayout.putConstraint(SpringLayout.NORTH, sendC,  	    0, SpringLayout.NORTH, getContentPane());
        slayout.putConstraint(SpringLayout.SOUTH, sendC,  	sendH, SpringLayout.NORTH, getContentPane());

        slayout.putConstraint(SpringLayout.SOUTH, showC,       0, SpringLayout.SOUTH, getContentPane());
        slayout.putConstraint(SpringLayout.NORTH, showC,       0, SpringLayout.SOUTH, sendC);

        Component[] components = {sendC, showC};
        for(Component c : components) {
            slayout.putConstraint(SpringLayout.WEST, c, 0, SpringLayout.WEST, getContentPane());
            slayout.putConstraint(SpringLayout.EAST, c, 0, SpringLayout.EAST, getContentPane());
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(400,500);
        //pack();
    
    }
  
    private void multicastMessage()
    {
        String m = messageField.getText();
        messageField.setText("");
        synchronized(mcgui.Lock.lock) {
            mc.cast(m);
        }
    }

    private class StressTimerTask extends java.util.TimerTask {
    
        private int times;
    
        public StressTimerTask(int times) {
            this.times = times;
        }

        public void run() {
            stress(times);
        }
    }

    /**
     * Start a thread that will send many messages at a given time
     */
    private void setStress()
    {
        try {
            int times = Integer.parseInt(stressNumber.getText());
            String m = stressTime.getText();
            java.text.SimpleDateFormat dayf = new java.text.SimpleDateFormat("yyyy:MM:dd");
            java.text.SimpleDateFormat timef = new java.text.SimpleDateFormat("H:mm:ss");
            java.text.SimpleDateFormat totf = new java.text.SimpleDateFormat("yyyy:MM:dd:H:mm:ss");

            /* Calculating alarm time */
            java.util.Date now = new java.util.Date(System.currentTimeMillis());
            java.util.Date alarm;
            if(m.trim().equals("")) {
                alarm = now;
            } else {
                java.util.Date d;
                try {
                    // Parse hh:mm
                    d = timef.parse(m);
                } catch(Exception e) {
                    // Parse hh:mm:ss
                    d = timef.parse(m+":00");
                }
                alarm = totf.parse(dayf.format(now)+":"+timef.format(d));
            }

            //System.out.println("Now:   "+now);
            //System.out.println("Alarm: "+alarm);

            /* Setting timer */
            java.util.Timer t = new java.util.Timer();
            StressTimerTask ts = new StressTimerTask(times);
            t.schedule(ts,alarm);
            stressTime.setText("");
        } catch(Exception e) {
            e.printStackTrace();
            stressTime.setText("Error!");
        }
    
    }

    /**
     * The function that the stress thread calls
     */
    private void stress(int times) 
    {
        for(int i = 0; i < times; i++) {
            synchronized(mcgui.Lock.lock) {
                mc.cast("stress-"+name+"-"+i);
            }
        }
    }
    
    /**
     * Concatenates a given string with all strings in a <code>List</code> of strings
     */
    private String concatStrings(String startstring,java.util.List<String> strings) {
        StringBuffer buf = new StringBuffer(startstring);
        for(String s : strings) {
            buf.append(s);
        }
        return buf.toString();
    }

    /* ======================================== */

    private static class GUIStarter implements Runnable {
        public GUI g;
        public Multicaster mc;
        public int name;
	
        public GUIStarter(Multicaster m,int n) {
            mc = m;
            name = n;
        }
    
        public void run() {
            g = new GUI(mc,name);
            g.setVisible(true);
        }
    }

    /**
     * Starts a GUI that uses the provided Multicaster module
     * @param m     Multicaster module to be used
     * @param name  Name of the client
     */
    static void startGUI(Multicaster m,final int name)
    {
        GUIStarter starter = new GUIStarter(m,name);
        boolean started = false;
        while(!started) {
            try {
                SwingUtilities.invokeAndWait(starter);
                started = true;
            } catch(InterruptedException e) {
                System.err.println("Problem starting GUI.");
            } catch(java.lang.reflect.InvocationTargetException e) {
                System.err.println("Problem starting GUI.");
            }
        }
        m.setUI(starter.g);
    }
    

  
    /**
     * @return the text to be shown in the message area
     */
    private String getDeliverText() {
        String firstline="";
        if(deliverMaxReached) {
            firstline="### Older messages not shown ###\n";
        }
        return concatStrings(firstline,recentMessages);
    }

    /**
     * @return the text to be shown in the debug area
     */
    private String getDebugText() {
        String firstline="";
        if(debugMaxReached) {
            firstline="### Older debug lines not shown ###\n";
        }
        return concatStrings(firstline,recentDebugs);
    }

    /**
     * Updates the display (called after changes to the message or debug
     * buffers.  Either notifies a running thread that more updates are
     * needed or starts a new thread if no other thread is running.
     */
    private void updateDisplay() {
        if(displayUpdating) {
            displayNeedUpdate = true;
        } else {
            displayUpdating = true;
            displayNeedUpdate = false;
      
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        synchronized(mcgui.Lock.lock) {
                            
                            boolean done = false;
                            while(!done) {
                                String mtext = getDeliverText();
                                receiveArea.setText(mtext);
                                receiveArea.setCaretPosition(mtext.length());
                                String dtext = getDebugText();
                                debugArea.setText(dtext);
                                debugArea.setCaretPosition(dtext.length());
                                
                                done = repaintDisplayDone();
                            }
                        }
                    }});
            
        }
    }

  
    /**
     * An updating thread needs to continue updating until this returns false.
     */
    private boolean repaintDisplayDone() {
        if(displayNeedUpdate) {
            displayNeedUpdate = false;
            return false;
        } else {
            displayUpdating = false;
            return true;
        }
    }


    /**
     * Deliver a message that is shown in the GUI
     * @param from     The sender of the message
     * @param message  The message to be delivered
     * @param info     Additional info to be shown to the user about that message
     */
    public void deliver(int from,String message,String info) 
    {
        synchronized(mcgui.Lock.lock) {
            if(info == null || info.trim().equals("")) {
                info = "";
            } else {
                info = " ["+info+"]";
            }
            if(recentMessages.size() == maxMessages) {
                deliverMaxReached = true;
                recentMessages.remove();
            }
            String line = from+":"+message+info+"\n";
            recentMessages.add(line);
            updateDisplay();
            if(deliverout != null) {
                deliverout.println(from+":"+message);
                deliverout.flush();
            }
        }
    }

    /**
     * Deliver a message that is shown in the GUI (without any extra info).
     * @param from     The sender of the message
     * @param message  The message to be delivered
     */
    public void deliver(int from,String message) 
    {
        deliver(from,message,null);
    }
  
    /**
     * Add a debug message that is shown in the GUI
     * @param string   The debug message to be shown
     */
    public synchronized void debug(String string) 
    {
        synchronized(mcgui.Lock.lock) {
            if(recentDebugs.size() == maxDebugs) {
                debugMaxReached = true;
                recentDebugs.remove();
            }
            String line = string+"\n";
            recentDebugs.add(line);
            updateDisplay();
        }
    }

    /**
     * Enables the GUI to start sending messages.
     */
    public void enableSending() 
    {
        sendButton.setEnabled(true);
        stressButton.setEnabled(true);
    }

    public static final long serialVersionUID = 0;
}
