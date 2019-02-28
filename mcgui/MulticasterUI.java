package mcgui;

/**
 * Interface that is implemented by the GUI that is used by a
 * Multicaster for deliveries and debug messages to the GUI.
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public interface MulticasterUI {

  /**
   * Deliver a message to the UI
   * @param from     The sender of the message
   * @param message  The message to be delivered
   * @param info     Additional information to be shown to the user about the message. Allowed to
   * be <code>null</code> if no additonal information is to be shown.
   */
  public void deliver(int from,String message,String info);

  /**
   * Deliver a message to the UI (without any additional information)
   * @param from     The sender of the message
   * @param message  The message to be delivered
   */
  public void deliver(int from,String message);

  /**
   * Add a debug message to the GUI
   * @param string   The debug message to be shown
   */
  public void debug(String string);

  /**
   * Enables the GUI to start sending messages when all the communications are set up.
   */
  public void enableSending();

}