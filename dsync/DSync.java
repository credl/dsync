
package dsync;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import logic.Synchronizer;

import datastructures.Action;
import datastructures.ActionGroup;
import datastructures.MultiProperties;

/**
 * Main class. Version 1.3.
 * @author Christoph Redl
 */
public class DSync {

	public static void main(String[] args){

		String versionString = "2.40, 02.11.2022";
		
		MultiProperties prop = null;
		List<ActionGroup> actions = null;
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		
		switch (args.length){
			case 4: // Two locations, a bidirectional flag and a reference date are given
				try {
					System.out.print("Parsing parameters ... ");	
					Boolean bidirectional = new Boolean(args[3]);
					Date referencedate = (DateFormat.getDateInstance(DateFormat.SHORT)).parse(args[4]);
					System.out.println("ok");
					System.out.println("Using sync date from command line: " + df.format(referencedate));
					System.out.println("Do not use file system cache");
					
					// Gather the actions necessary to synchronize the locations					
					System.out.println("Collecting sync actions ... ");				
					Synchronizer sync = new Synchronizer();
					actions = sync.synchronize(args[0], args[1], bidirectional, referencedate);
					System.out.println("Sync actions ... ok" + " (" + sync.getSyncTime() + " seconds)");
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
					e.printStackTrace();
				}
				break;
				
			case 3: // Two locations and a bidirectional flag are given
				try {
					System.out.print("Parsing parameters ... ");		
					Boolean bidirectional = new Boolean(args[2]);
					System.out.println("ok");
					Date referenceDate = new Date(0);
					System.out.println("Using default sync date: " + df.format(referenceDate));
					System.out.println("Do not use file system cache");

					// Gather the actions necessary to synchronize the locations
					System.out.println("Collecting sync actions ... ");				
					Synchronizer sync = new Synchronizer();
					actions = sync.synchronize(args[0], args[1], bidirectional, referenceDate);
					System.out.println("Sync actions ... ok" + " (" + sync.getSyncTime() + " seconds)");
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
					e.printStackTrace();
				}
				break;
				
			case 2: // Two locations are given
				try {
					// Gather the actions necessary to synchronize the locations
					System.out.println("Collecting sync actions ... ");
					Date referenceDate = new Date(0);
					System.out.println("Using default sync date: " + df.format(referenceDate));
					System.out.println("Do not use file system cache");

					Synchronizer sync = new Synchronizer();
					actions = sync.synchronize(args[0], args[1], true, referenceDate);
					System.out.println("Sync actions ... ok" + " (" + sync.getSyncTime() + " seconds)");
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
					e.printStackTrace();
				}
				break;
				
			case 1:	// sync file ist given
				if (args[0].compareTo("-version") == 0){
					System.out.println("Version: " + versionString);
				}else{
					try {
						// Read the locations to synchronize
						System.out.print("Reading sync file \"" + args[0] + "\" ... ");
						prop = new MultiProperties();
						prop.load(new FileInputStream(args[0]));
						System.out.println("ok");
	
						// Read sync date
						// Read reference date: either directly from the config file or from external lastsync-file.
						// Priority of sync date sources is as follows:
						//		3. (lowest) syncdate stores as "syncdate" in sync file
						//		2. syncdate stored in the file references by "lastsyncfile" in sync file
						//		1. (highest) "referencedate" in syncfile
						Date referenceDate = null;
						if (prop.containsKey("referencedate")){
							referenceDate = DateFormat.getDateInstance(DateFormat.SHORT).parse(prop.getProperty("referencedate"));
							System.out.println("Using syncdate \"referencedate\" from sync file: " + df.format(referenceDate));
						}else if (prop.containsKey("lastsyncfile")){
							BufferedReader br = new BufferedReader(new FileReader(prop.getProperty("lastsyncfile")));
							referenceDate = new Date(Long.parseLong(br.readLine()));
							br.close();
							System.out.println("Using syncdate from lastsyncfile \"" + prop.getProperty("lastsyncfile") + "\": " + df.format(referenceDate));
						}else if (prop.containsKey("lastsync")){
							referenceDate = new Date(Long.parseLong(prop.getProperty("lastsync")));
							System.out.println("Using sync date \"lastsync\" from sync file: " + df.format(referenceDate));
						}else{
							referenceDate = new Date(0);
							System.out.println("Using default reference date: " + df.format(referenceDate));
						}

						boolean useCache = prop.containsKey("usecache") && new Boolean(prop.getProperty("usecache")).booleanValue();
						boolean ignoreSymbolicLinks = !prop.containsKey("ignoresymboliclinks") || new Boolean(prop.getProperty("ignoresymboliclinks")).booleanValue();
						System.out.println(useCache ? "Using file system cache" : "Do not use file system cache");
						System.out.println(ignoreSymbolicLinks ? "Ignoring symbolic links" : "Do not ignore symbolic links");				
						
						// Gather the actions necessary to synchronize the locations
						System.out.println("Collecting sync actions ... ");
						Synchronizer sync = new Synchronizer();
						actions = sync.synchronize(args[0], referenceDate);
						System.out.println("Sync actions ... ok" + " (" + sync.getSyncTime() + " seconds)");			
					} catch (Exception e) {
						System.out.println("Error: " + e.getMessage());
						e.printStackTrace();
					}
				}
				break;
			
			default:
				System.out.println("dsync\n-----");
				System.out.println("");
				
				System.out.println("dsync is a utility for two-way synchronization of directories.");
				System.out.println("");
				
				System.out.println("Synchronization is based on the following algorithm. If there is difference between a file or directory in the two locations (i.e. it exists in one location, but not in the other one), the last modified date of the element is compared with a reference date. If it is newer than the reference date, the element is copied to the location where it is missing.");
				System.out.println("");
				
				System.out.println("If it is older than the reference date, it is deleted from the location where it exists. The reference date is the earliest possible date (01-01-1970) if no other one is specified. In case a sync file is used, the last synchronization date is automatically stored and used as reference date. In case no sync file is used, a reference date may be given as parameter.");
				System.out.println("");
				
				System.out.println("Usage: You can use the program in the following ways:\n   - pass a sync file as parameter (see below)\n   - pass two locations (directories) as parameters\n   - pass two locations and bidirectional flag (true/false)\n   - pass two locations, bidirectional flag and reference date");
				System.out.println("");
				
				System.out.println("Default values (for missing parameters) are: bidirectional, reference date is January 1, 1970, 01:00:00 GMT");
				System.out.println("");
				
				System.out.println("Sync file template:\n     synclocations=d1<-->d2;d3-->d4;d5<--d6\n     prefix1=/home/user\n     prefix2=/home/user\n     filter=Name:.*;;Path:.*\n     filterfile=[path]\n     referencedate=01.01.2000\n     lastsync=0     \n     lastsyncfile=[path]\n     usecaching=[boolean]\n     ignoresymboliclinks=[boolean]");
				System.out.println("(Note: By default, the reference date is the last synchronization date. This can be overwritten by specifying attribute 'referencedate'.)");
				break;
		}
		
		// Acions were collected
		try {
			if (actions != null){
				if (actions.size() > 0){
					// Let the user decide which actions to execute
					String input = "";
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					boolean shortoutput = true;
					
					// Show action list to the user
					System.out.println("Action list is:");
					printActions(actions, shortoutput);
					System.out.println(	"Enter 'ok' to execute the selected actions or 'cancel' to abort.\n" + 
										"Enter [action-character] ([list of numbers] | * ) to select or unselect actions\n" +
										"Enter long/short to toggle output details");
					while (input.compareTo("ok") != 0 && input.compareTo("cancel") != 0){
	
						// Input handling
						System.out.print(": ");
						input = br.readLine();

						try{
							if (input.compareTo("ok") == 0 || input.compareTo("cancel") == 0){
								System.out.println("");
							}else if (input.compareTo("long") == 0){
								shortoutput = false;

								// Show modified action list
								printActions(actions, shortoutput);
							}else if (input.compareTo("short") == 0){
								shortoutput = true;

								// Show modified action list
								printActions(actions, shortoutput);
							}else if (input.startsWith("show ")){
								String[] split = input.split(" ");
								if (split.length != 3) throw new Exception("split expectes 2 arguments (lower and upper bound)");
								int lowerbound = Integer.parseInt(split[1]);
								int upperbound = Integer.parseInt(split[2]);
								if (lowerbound < 1 || upperbound > actions.size()) throw new Exception("Invalid bounds");

								// Show partial action list
								printActions(actions, shortoutput, lowerbound, upperbound, false, ' ');
							}else if (input.startsWith("filter ")){
								String[] splitf = input.split(" ");
								if (splitf.length != 2 || splitf[1].length() != 1) throw new Exception("filter expectes 1 argument (action character)");
								char filteractionchar = splitf[1].charAt(0);

								// Show partial action list
								printActions(actions, shortoutput, 1, actions.size(), true, filteractionchar);
							}else{
								char actionChar = input.charAt(0);						
								String[] nrs = input.substring(1).split(",");
								for (int j = 0; j < nrs.length; j++){
									setAction(actions, actionChar, nrs[j]);
								}

								// Show modified action list
								printActions(actions, shortoutput);
							}
		
						}catch(Exception e){
							System.out.println("");
							System.out.println("Error: " + e.getMessage());
						}
					}
			
					// ok or cancel?
					if (input.compareTo("ok") == 0){
						// Actually execute the actions on the file system
						System.out.print("Processing actions ... \n");
						String log = Action.executeActionList(ActionGroup.getSelectedActions(actions));
						System.out.println("ok");
						
						// Write the new last sync date
						df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
						Date now = new Date();
						if (prop.containsKey("lastsyncfile")){
							System.out.print("Writing new sync date (" + df.format(now) + ") into \"" + prop.getProperty("lastsyncfile") + "\" ... ");
							PrintWriter pw = new PrintWriter(prop.getProperty("lastsyncfile"));
							pw.println((new Long(now.getTime())).toString());
							pw.close();
						}else{
							System.out.print("Writing new sync date (" + df.format(now) + ") into sync file ... ");
							prop.setProperty("lastsync", (new Long(now.getTime())).toString());
							prop.store(new FileOutputStream(args[0]), "");
						}
						System.out.println("ok");
						
						System.out.println("");
						System.out.println("Log is:");
						System.out.println(log);
						
						System.out.println("");
						System.out.println("Finished!");
					}else{
						System.out.println("Cancelled, no actions were performed");						
					}
				}else{
					System.out.println("Locations are synchron! Nothing to do.");
					System.out.flush();
				}
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Sets a new action for a given action group or a range of action groups
	 * @param actions
	 * @param actionChar
	 * @param nr: Either a single action id or a range of action ids
	 * @param IllegalArgumentException In case of invalid ids
	 */
	private static void setAction(List<ActionGroup> actions, char actionChar, String nr) throws IllegalArgumentException{
		// Range of ids?
		int lowerbound, upperbound;
		try{
			if (nr.compareTo("*") == 0){
				lowerbound = 1;
				upperbound = actions.size();
			}else if (nr.contains("-")){
				// Extract lower and upper bound
				lowerbound = Integer.parseInt(nr.substring(0, nr.indexOf("-")));
				upperbound = Integer.parseInt(nr.substring(nr.indexOf("-") + 1));
	
			}else{
				// Single id
				lowerbound = Integer.parseInt(nr);
				upperbound = Integer.parseInt(nr);
			}
		}catch(NumberFormatException nfe){
			throw new IllegalArgumentException(nr + " is not a valid number or range");
		}
		if (lowerbound < 1 || lowerbound > actions.size()) throw new IllegalArgumentException(lowerbound + " is out of bounds");
		if (upperbound < 1 || upperbound > actions.size()) throw new IllegalArgumentException(upperbound + " is out of bounds");
		if (upperbound <  lowerbound) throw new IllegalArgumentException(nr + " is invalid since " + upperbound + "<" + lowerbound);
		
		// Check if all actions in range support the selected action character
		for (int index = lowerbound; index <= upperbound; index++){
			if (!(actions.get(index - 1).supportsAction(actionChar) || actionChar == '+' || actionChar == '-')) throw new IllegalArgumentException("Action '" + actionChar + "' is not supported by all selected actions");
		}
		
		// Actually set the actions
		for (int index = lowerbound; index <= upperbound; index++){
			// Recognize shortcut for unselection and selection of first action
			switch(actionChar){
				case '+':
					actions.get(index - 1).selectAction(0);
					break;
				case '-':
					actions.get(index - 1).unselectAction();
					break;
				default:
					actions.get(index - 1).selectAction(actionChar);
					break;
			}
		}
	}
	
	/**
	 * Prints a list of action groups and marks the currently selected actions 
	 * @param actionGroup
	 */
	private static void printActions(List<ActionGroup> actionGroup, boolean shortoutput){
		printActions(actionGroup, shortoutput, 1, actionGroup.size(), false, ' ');
	}
	private static void printActions(List<ActionGroup> actionGroup, boolean shortoutput, int lowerbound, int upperbound, boolean filterbychar, char filteractionchar){
		int longestLine = 0;
		StringBuilder output = new StringBuilder("");
		for (int i = lowerbound; i <= upperbound; i++){
			ActionGroup currentActionGroup = actionGroup.get(i - 1);
			
			if (!filterbychar || (currentActionGroup.getSelectedActionChar() != null && currentActionGroup.getSelectedActionChar().charValue() == filteractionchar)){
				String nr = (new Integer(i)).toString();
				output.append("[" + nr + "] ");
				String[] lines = currentActionGroup.toString(shortoutput).split("\n"); 
				for (int lineIndex = 0; lineIndex < lines.length; lineIndex++){
					if (lineIndex > 0){
						output.append("\n");
						for (int j = 0; j < nr.length() + 3; j++) output.append(" ");
					}
					output.append(lines[lineIndex]);
					longestLine = Math.max(longestLine, lines[lineIndex].length() + nr.length() + 3);
				}
				output.append("\n");
			}
		}
		for (int j = 0; j < longestLine; j++) System.out.print("-");
		System.out.println("");
		System.out.println(output.toString());
		for (int j = 0; j < longestLine; j++) System.out.print("-");
		System.out.println("");
		System.out.flush();
	}
	
}
