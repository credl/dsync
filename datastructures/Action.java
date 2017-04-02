package datastructures;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Stores a filesystem action.
 * @author Christoph Redl
 */
public class Action {

	public enum Type{
		CreateDirectory,
		CreateFile,
		CopyDirectory,
		CopyFile,
		DelAndCopyDirectory,
		DelAndCopyFile,
		DeleteDirectory,
		DeleteFile,
		Conflict
	}
	
	private Type type;
	private String source, destination;
	
	// Just for performance enhancement
	private String sourceInfo = "";
	private String destInfo = "";
	private boolean infosCached = false;
	
	/**
	 * Constructor
	 * @param type
	 * @param source
	 * @param destination
	 * @throws IllegalArgumentException If too few or too many parameters are provided (depending on the action type). 
	 */
	public Action(Type type, String source) throws IllegalArgumentException{
		if (type == Type.DelAndCopyDirectory || type == Type.DelAndCopyFile || type == Type.CopyDirectory || type == Type.CopyFile) throw new IllegalArgumentException("Copy action needs 2 arguments");
		setType(type);
		setSource(source);
	}
	
	/**
	 * Constructor
	 * @param type
	 * @param source
	 * @param destination
	 * @throws IllegalArgumentException If too few or too many parameters are provided (depending on the action type). 
	 */
	public Action(Type type, String source, String destination) throws IllegalArgumentException{
		if (type != Type.DelAndCopyDirectory && type != Type.DelAndCopyFile && type != Type.CopyDirectory && type != Type.CopyFile && type != Type.Conflict) throw new IllegalArgumentException("Only copy action needs 2 arguments");
		setType(type);
		setSource(source);
		setDestination(destination);
	}
	
	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}
	
	/**
	 * Actually executes a given list of actions on the filesystem (in the given order).
	 * @param actions
	 * @return log
	 */
	public static String executeActionList(List<Action> actions){
		StringBuilder sb = new StringBuilder();
		boolean firstline = true;
		int i = 1;
		int count = actions.size();
		for (Action action : actions){
			System.out.println("  " + (i++) + " of " + count + "\n" + action.toString());
			if (!firstline) sb.append("\n");
			firstline = false;
			try{
				sb.append("# " + action.execute());
			}catch(Exception e){
				sb.append("! Failed to execute action: [" + action.toString() + "], " + e.getMessage());
			}
		}
		return sb.toString();
	}
	
	/**
	 * Actually executes this action on the filesystem.
	 * @return log
	 */
	public String execute(){
		switch(getType()){
			case CreateFile:
				createFile(getSource()); 
				return "Created file \"" + getSource() + "\"";
			case CreateDirectory:
				createFile(getSource()); 
				return "Created directory \"" + getSource() + "\"";
			case CopyFile:
				copy(getSource(), getDestination());
				return "Copied file \"" + getSource() + "\" to \"" + getDestination() + "\"";
			case CopyDirectory:
				copy(getSource(), getDestination());
				return "Copied directory \"" + getSource() + "\" to \"" + getDestination() + "\"";
			case DelAndCopyFile:
				delete(getDestination());
				copy(getSource(), getDestination());
				return "Deleted \"" + getDestination() + "\" and copied file \"" + getSource() + "\" to \"" + getDestination() + "\"";
			case DelAndCopyDirectory: 
				delete(getDestination());
				copy(getSource(), getDestination());
				return "Deleted \"" + getDestination() + "\" and copied directory \"" + getSource() + "\" to \"" + getDestination() + "\"";
			case DeleteFile:
				delete(getSource());
				return "Deleted file \"" + getSource() + "\"";
			case DeleteDirectory:
				delete(getSource());
				return "Deleted directory \"" + getSource() + "\"";
			default:
				return "";
		}
	}

	/**
	 * String representation of this actoin.
	 * @return String
	 */
	public String toString(){
		return toString(false);
	}
	
	/**
	 * String representation of this actoin.
	 * @param shortoutput boolean If true, output will be shortened.
	 * @return String
	 */
	public String toString(boolean shortoutput){
		// Form additional info for modification date and file size
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

		if (!infosCached){
			switch(getType()){
				case CreateFile: 
				case CreateDirectory:
				case CopyFile:
				case DelAndCopyFile:
					if (new File(getSource()).exists()) sourceInfo = "(" + formatFileSize(new File(getSource()).length()) + ", " + df.format(new Date(new File(getSource()).lastModified())) + ")";
					if (new File(getDestination()).exists()) destInfo = "(" + formatFileSize(new File(getDestination()).length()) + ", " + df.format(new Date(new File(getDestination()).lastModified())) + ")";
					break;
				case CopyDirectory: 
				case DelAndCopyDirectory: 
					if (new File(getSource()).exists()) sourceInfo = "(" + df.format(new Date(new File(getSource()).lastModified())) + ")";
					if (new File(getDestination()).exists()) destInfo = "(" + df.format(new Date(new File(getDestination()).lastModified())) + ")";
					break;
				case DeleteFile:
					if (new File(getSource()).exists()) sourceInfo = "(" + formatFileSize(new File(getSource()).length()) + ", " + df.format(new Date(new File(getSource()).lastModified())) + ")";
					break;
				case DeleteDirectory:
					if (new File(getSource()).exists()) sourceInfo = "(" + formatFileSize(new File(getSource()).length()) + ", " + df.format(new Date(new File(getSource()).lastModified())) + ")";
					break;
				case Conflict:
					if (new File(getSource()).exists()) sourceInfo = "(" + formatFileSize(new File(getSource()).length()) + ", " + df.format(new Date(new File(getSource()).lastModified())) + ")";
					if (new File(getDestination()).exists()) destInfo = "(" + formatFileSize(new File(getDestination()).length()) + ", " + df.format(new Date(new File(getDestination()).lastModified())) + ")";
					break;
				default:
			}
			infosCached = true;
		}
		
		// Generate string representation
		if (shortoutput){
			final int consoleWidth = 72;
			switch(getType()){
				case CreateFile: 
				case CreateDirectory:
					return shortenPaths("ADD \"[0]\"", new String[]{getSource()}, consoleWidth) + "\n" + sourceInfo;
				case CopyFile:
					String out1 = shortenPaths("\"[0]\" --> \"[1]\"", new String[]{getSource(), getDestination()}, consoleWidth);
					String inf1 = "";
					for (int j = 0; j < out1.indexOf("-->") - (sourceInfo.length() + 1); j++){
						inf1 = inf1 + " ";
					}
					inf1 = inf1 + sourceInfo + "     " + destInfo;
					return out1 + "\n" + inf1;
				case CopyDirectory:
					String out2 = shortenPaths("\"[0]\" ==> \"[1]\"", new String[]{getSource(), getDestination()}, consoleWidth);
					String inf2 = "";
					for (int j = 0; j < out2.indexOf("==>") - (sourceInfo.length() + 1); j++){
						inf2 = inf2 + " ";
					}
					inf2 = inf2 + sourceInfo + "     " + destInfo;
					return out2 + "\n" + inf2;
				case DelAndCopyFile:
					String out3 = shortenPaths("\"[0]\" --> \"[1]\"", new String[]{getSource(), getDestination()}, consoleWidth);
					String inf3 = "";
					for (int j = 0; j < out3.indexOf("-->") - (sourceInfo.length() + 1); j++){
						inf3 = inf3 + " ";
					}
					inf1 = inf3 + sourceInfo + "     " + destInfo;
					return out3 + "\n" + inf1;
				case DelAndCopyDirectory: 
					String out4 = shortenPaths("\"[0]\" ==> \"[1]\"", new String[]{getSource(), getDestination()}, consoleWidth);
					String inf4 = "";
					for (int j = 0; j < out4.indexOf("==>") - (sourceInfo.length() + 1); j++){
						inf4 = inf4 + " ";
					}
					inf4 = inf4 + sourceInfo + "     " + destInfo;
					return out4 + "\n" + inf4;
				case DeleteFile:
				case DeleteDirectory:
					return shortenPaths("DEL \"[0]\"", new String[]{getSource()}, consoleWidth) + "\n" + sourceInfo;
				case Conflict:
					return shortenPaths("!! \"[0]\" / \"[1]\" (skip)", new String[]{getSource(), getDestination()}, consoleWidth);
				default:
					return "";
			}
		}else{
			switch(getType()){
				case CreateFile: 
					return "Create file \"" + getSource() + "\"";
				case CreateDirectory:
					return "Create directory \"" + getSource() + "\"";
				case CopyFile:
					return "Copy file \"" + getSource() + "\" " + sourceInfo + " to \"" + getDestination() + "\" " + destInfo;
				case CopyDirectory: 
					return "Copy directory \"" + getSource() + "\" " + sourceInfo + " to \"" + getDestination() + "\" " + destInfo;
				case DelAndCopyFile:
					return "Replace \"" + getDestination() + "\" by a copy of \"" + getSource() + "\"";
				case DelAndCopyDirectory: 
					return "Replace \"" + getDestination() + "\" by a copy of \"" + getSource() + "\"";
				case DeleteFile:
					return "Delete file \"" + getSource() + "\" " + sourceInfo + "";
				case DeleteDirectory:
					return "Delete directory \"" + getSource() + "\" " + sourceInfo;
				case Conflict:
					return "Conflict: \"" + getSource() + "\" " + sourceInfo + " / \"" + getDestination() + "\" " + destInfo + " (do nothing)";
				default:
					return "";
			}
		}
	}
	
	/**
	 * Formats a given file size (in bytes) to make it human readable.
	 * @param fs
	 * @return String
	 */
	private String formatFileSize(long memReq){
		double mr = (double)memReq;
		if (mr >= 1024.0){
			mr = roundmemreq(mr / 1024.0, 2);
			if (mr >= 1024.0f){
				mr = roundmemreq(mr / 1024.0f, 2);
				if (mr >= 1024.0){
					mr = roundmemreq(mr / 1024.0f, 2);
					return "" + mr + " GiB";
				}else{
					return "" + mr + " MiB";
				}
			}else{
				return "" + mr + " KiB";
			}
		}else{
			return "" + mr + " B";
		}
	}
	
	/**
	 * Rounds n to d decimal numbers after the .
	 * @param n
	 * @param d
	 * @return Rounded value
	 */
	private double roundmemreq(double n, int d){
		return Math.round(n * Math.pow(10, d)) / Math.pow(10, d);
	}
	
	/**
	 * Cuts off pieces of paths until they fit into a given line width.
	 * @param template
	 * @param paths
	 * @param maxLen
	 * @return String Shortened path
	 */
	private String shortenPaths(String template, String[] paths, int maxLen){
		// First try to insert full paths into the template
		boolean fin = false;
		int nextCutOffIndex = 0;
		String currenttry = "";
		int bc = 0;
		while (!fin){
			// Fill slots
			currenttry = template;
			for (int i = 0; i < paths.length; i++){
				currenttry = currenttry.replace("[" + i + "]", paths[i]);
			}
			
			// Short enough?
			if (currenttry.length() <= maxLen){
				// Yes
				fin = true;
			}else{
				// No: Cut off pieces of the paths
				
				// Cut off one directory if possible (beginning at the topmost one)
				if (paths[nextCutOffIndex].indexOf(File.separator) >= 0 && paths[nextCutOffIndex].indexOf(File.separator) < paths[nextCutOffIndex].length() - 1){
					// Cut off
					paths[nextCutOffIndex] = "..." + paths[nextCutOffIndex].substring(paths[nextCutOffIndex].indexOf(File.separator) + 1);
					nextCutOffIndex = (nextCutOffIndex + 1) % paths.length;
					bc = 0;
				}else{
					nextCutOffIndex = (nextCutOffIndex + 1) % paths.length;
					bc++;
					if (bc >= paths.length){
						// Cutting directories is not possible anymore; we are not able to keep the string below he maxlength
						// Cut single characters until we can't anymore or we are below the limit
						nextCutOffIndex = 0;
						bc = 0;
						maxLen -= 6;	// Reserve place for 2 times "..."
						for (int j = 0; (j < currenttry.length() - maxLen) && bc < paths.length; j++){
							if (paths[nextCutOffIndex].length() > 0){
								paths[nextCutOffIndex] = paths[nextCutOffIndex].substring(1);
								bc = 0;
							}else{
								bc++;
							}
							nextCutOffIndex = (nextCutOffIndex + 1) % paths.length;
						}
						// Fill slots
						currenttry = template;
						for (int i = 0; i < paths.length; i++){
							currenttry = currenttry.replace("[" + i + "]", "..." + paths[i]);
						}
						fin = true;	
					}
				}
			}
		}
		return currenttry;
	}

	// ------------------------------ Low-Level file operations ------------------------------
	private static void createFile(String f){
		try {
			(new File(f)).createNewFile();
		} catch (Exception e) {}
	}

	private static void createDirectory(String f){
		(new File(f)).mkdir();
	}
	
	private static void copy(String source, String dest){
		try {
			if ((new File(source)).isFile()){
				long fs = (new File(source)).length();
				
				// File copy
				FileInputStream fis = new FileInputStream(source);
				FileOutputStream fos = new FileOutputStream(dest);
				byte[] bytes = null;
				if (fs > 1024 * 1024 * 50) bytes = new byte[1024 * 1024 * 50];
				else if (fs > 1024 * 1024 * 25) bytes = new byte[1024 * 1024 * 25];
				else if (fs > 1024 * 1024 * 10) bytes = new byte[1024 * 1024 * 10];
				else if (fs > 1024 * 1024 * 5) bytes = new byte[1024 * 1024 * 5];
				else if (fs > 1024 * 1024) bytes = new byte[1024 * 1024];
				else bytes = new byte[1024];
				int count = 0;
				while ((count = fis.read(bytes)) > 0){
					fos.write(bytes, 0, count);
				}
				fos.close();
				fis.close();
				
				// Essential: Take last modification date from source!
				File sourceFile = new File(source);
				File destinationFile = new File(dest);
				if (destinationFile.setLastModified(sourceFile.lastModified()) == false){
					System.err.println("Fatal: Could not modify date of \"" + destinationFile.getAbsolutePath() + "\".\nPlease fix this file manually, check access rights and restart\nTerminate to avoid further errors.");
					System.exit(1);
				}
			}else{
				// Make sure that the destination exists
				createDirectory(dest);
				
				// Copy recursively
				String[] children = (new File(source)).list();
				for (int i = 0; i < children.length; i++){
					copy(source + File.separator + children[i], dest + File.separator + children[i]);
				}
			}
		} catch (Exception e) {}
	}

	private static void delete(String f){
		File file = new File(f);
		if (file.isFile()){
			file.delete();
		}else{
			// Delete recursively
			String[] children = file.list();
			for (int i = 0; i < children.length; i++){
				delete(file.getAbsolutePath() + File.separator + children[i]);
			}
			file.delete();
		}
	}
}
