package datastructures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Stores a list of actions: a primary action and one or more alternatives 
 * @author Christoph Redl
 */
public class ActionGroup implements Iterable<Action>{

	private String description = "";
	private List<Character> actionChars = new ArrayList<Character>();
	private List<Action> actions = new ArrayList<Action>();
	private int selectedAction = -1;
	
	/**
	 * Initialize an empty list
	 */
	public ActionGroup(){
		setDescription("");
	}

	/**
	 * Initialize an empty list
	 * @param description
	 */
	public ActionGroup(String description){
		setDescription(description);
	}
	
	/**
	 * Initialize a list with one action
	 * @param c
	 * @param a
	 */
	public ActionGroup(char c, Action a){
		actionChars.add(c);
		actions.add(a);
		selectedAction = 0;
	}

	/**
	 * Initialize a list with one action
	 * @param description
	 * @param c
	 * @param a
	 */
	public ActionGroup(String description, char c, Action a){
		this(c, a);
		setDescription(description);
	}
	
	/**
	 * Initialize a list with two actions
	 * @param c1
	 * @param c2
	 * @param a1
	 * @param a2
	 */
	public ActionGroup(char c1, char c2, Action a1, Action a2){
		actionChars.add(c1);
		actionChars.add(c2);
		actions.add(a1);
		actions.add(a2);
		selectedAction = 0;
	}

	/**
	 * Initialize a list with one action
	 * @param description
	 * @param c1
	 * @param c2
	 * @param a1
	 * @param a2
	 */
	public ActionGroup(String description, char c1, char c2, Action a1, Action a2){
		this(c1, c2, a1, a2);
		setDescription(description);
	}
	
	/**
	 * Initialize a list with three actions
	 * @param c1
	 * @param c2
	 * @param c3
	 * @param a1
	 * @param a2
	 * @param a3
	 */
	public ActionGroup(char c1, char c2, char c3, Action a1, Action a2, Action a3){
		actionChars.add(c1);
		actionChars.add(c2);
		actionChars.add(c3);
		actions.add(a1);
		actions.add(a2);
		actions.add(a3);
		selectedAction = 0;
	}
	
	/**
	 * Initialize a list with three actions
	 * @param description
	 * @param c1
	 * @param c2
	 * @param c3
	 * @param a1
	 * @param a2
	 * @param a3
	 */
	public ActionGroup(String description, char c1, char c2, char c3, Action a1, Action a2, Action a3){
		actionChars.add(c1);
		actionChars.add(c2);
		actionChars.add(c3);
		actions.add(a1);
		actions.add(a2);
		actions.add(a3);
		selectedAction = 0;
		setDescription(description);
	}
	
	/**
	 * Changes the description of this action group.
	 * @param description
	 */
	public void setDescription(String description){
		this.description = description;
	}
	
	/**
	 * Adds an additional action
	 * @param c
	 * @param a
	 */
	public void addAction(char c, Action a){
		actionChars.add(c);
		actions.add(a);
	}
	
	/**
	 * Returns the currently selected action (if any)
	 * @return Action The currently selected action or null, if there is no action selected
	 */
	public Action getSelectedAction(){
		return selectedAction == -1 ? null : actions.get(selectedAction);
	}
	
	/**
	 * Returns the action character of the currently selected action (if any)
	 * @return Action The character of the currently selected action or null, if there is no action selected
	 */
	public Character getSelectedActionChar(){
		return selectedAction == -1 ? null : actionChars.get(selectedAction);
	}

	/**
	 * Selects a certain action given by it's index.
	 * @param index: The 0-based index of the selected action
	 * @throws IndexOutOfBoundsException If the given index is out of bounds
	 */
	public void selectAction(int index) throws IndexOutOfBoundsException{
		if (index < 0 || index >= actions.size()) throw new IndexOutOfBoundsException();
		selectedAction = index;
	}
	
	/**
	 * Unselects all actions.
	 */
	public void unselectAction(){
		selectedAction = -1;
	}

	/**
	 * Selects a certain action given by it's action char.
	 * @param char
	 * @throws IllegalArgumentException If the given action char was not found
	 */
	public void selectAction(char c) throws IllegalArgumentException{
		// Search for the index of this action character
		Iterator<Character> accIt = actionChars.iterator();
		int index = -1;
		int i = -1;
		while (accIt.hasNext()){
			i++;
			if (accIt.next() == c){
				index = i;
				break;
			}
		}
		
		// Check if the action character was found
		if (index == -1){
			throw new IllegalArgumentException("Action character '" + c + "' is invalid for this action");
		}else{
			selectedAction = index;
		}
	}
	
	/**
	 * Retrieves a certain action from the action group.
	 * @param index
	 * @return Action
	 */
	public Action getAction(int index){
		if (index < 0 || index >= actions.size()) throw new IndexOutOfBoundsException();
		return actions.get(index);
	}
	
	/**
	 * Returns the number of actions in this list.
	 * @return int
	 */
	public int getActionCount(){
		return actions.size();
	}
	
	/**
	 * Returns an action iterator
	 * @return Iterator<Action>
	 */
	@Override
	public Iterator<Action> iterator() {
		return actions.iterator();
	}

	/**
	 * Checks if this action group suppors the given action.
	 * @param c An action character
	 * @return boolean
	 */
	public boolean supportsAction(char c){
		return this.actionChars.contains(c);
	}
	
	/**
	 * Delivers a string represenation of this action group.
	 * @return String
	 */
	public String toString(){
		return toString(false);
	}
	
	/**
	 * Delivers a string represenation of this action group.
	 * @param boolean shortoutput If true, output will be shortened 
	 * @return String
	 */
	public String toString(boolean shortoutput){
		// Show all alternatives
		Iterator<Action> acIt = iterator();
		Iterator<Character> accIt = actionChars.iterator();
		String str = description;
		boolean firstLine = (description.length() == 0);
		int i = 0;
		while (acIt.hasNext()){
			if (!firstLine){
				str = str + "\n";
			}
			firstLine = false;
			String[] actionStringRep = acIt.next().toString(shortoutput).split("\n");
			
			// Is this action selected?
			String shift = "";
			if (i == selectedAction){
				// Yes: Show paranthesis 
				shift = "(" + accIt.next().toString() + ") ";
			}else{
				// No: Don't show paranthesis 
				shift = " " + accIt.next().toString() + "  ";				
			}
			String blankShift = "";
			for (int j = 0; j < shift.length(); j++) blankShift = blankShift + " "; 
			str = str + shift;
			
			// Add string representation of action
			for (int j = 0; j < actionStringRep.length; j++){
				str = str + (j > 0 ? "\n" + blankShift : "") + actionStringRep[j];
			}
			i++;
		}
		return str;
	}
	
	/**
	 * Returns the list of actually selected actions out of a list of action groups.
	 * @param list
	 * @return List<Action>
	 */
	public static List<Action> getSelectedActions(List<ActionGroup> list){
		List<Action> resultList = new ArrayList<Action>();
		Iterator<ActionGroup> it = list.iterator();
		while (it.hasNext()){
			Action ac = it.next().getSelectedAction();
			if (ac != null) resultList.add(ac);
		}
		return resultList;
	}
}