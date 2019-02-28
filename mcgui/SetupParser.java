package mcgui;

import java.io.*;
import java.util.ArrayList;

/**
 * Class that provides functions for parsing a setup file
 *
 * @author Andreas Larsson &lt;larandr@chalmers.se&gt;
 */
public class SetupParser {

  /**
   * @param fname   The filename of the file that includes the setup information
   * @return        An ArrayList with, for each line of the file, an array of Strings with the elements that were ':'-separated on that line.
   */
  public static ArrayList<String[]> parseFile(String fname) throws IOException {    
      ArrayList<String[]> info = new ArrayList<String[]>();
      BufferedReader in = new BufferedReader(new FileReader(fname));
      String s = in.readLine();
      while(s != null) {
	info.add(s.split(":"));
	s = in.readLine();
      }
      return info;
  }

}
