package logic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import datastructures.Action;
import datastructures.ActionGroup;
import datastructures.MultiProperties;

/**
 * Synchronizes two directories. The method does not apply any file operations. Rather it generates a list of suggested modifications.
 * @author Christoph Redl
 */
public class Synchronizer{

	MultiProperties prop;
	long syncTime;

	/**
	 * Extends java.io.File by a file system cache for faster traversal.
	 */
	static class CachedFile extends File{
		java.util.Set<String> ordinaryFiles;
		java.util.Set<String> directories;
		java.util.HashMap<String, LinkedList<String> > children;
		java.util.HashMap<String, Long> lastmodified;

		private class ProcessFile extends SimpleFileVisitor<Path>{
			void addToCache(Path p){
				if (p.getParent() != null){
					String parent = p.getParent().toString();
					String child = p.getName(p.getNameCount() - 1).toString();
					if (!children.containsKey(parent)){
						children.put(parent, new LinkedList<String>());
					}
					children.get(parent).add(child);
				}
			}
		
			@Override
			public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException{
				ordinaryFiles.add(aFile.toString());
				lastmodified.put(aFile.toString(), java.nio.file.Files.getLastModifiedTime(aFile).toMillis());
				addToCache(aFile);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path aDir, BasicFileAttributes aAttrs) throws IOException{
				directories.add(aDir.toString());
				addToCache(aDir);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException ioe){
				return ioe == null ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
			}
			
			@Override
			public FileVisitResult postVisitDirectory(Path aDir, IOException ioe){
				return ioe == null ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
			}
		}
		
		public CachedFile(String filename){
			super(filename);

			try{
				if (super.isDirectory()){
					System.out.print("   Setting up cache for " + filename + " ... ");

					ordinaryFiles = new java.util.HashSet<String>();
					directories = new java.util.HashSet<String>();
					children = new java.util.HashMap<String, LinkedList<String> >();
					lastmodified = new java.util.HashMap<String, Long>();
				
					FileVisitor<Path> fileProcessor = new ProcessFile();
					Files.walkFileTree(Paths.get(getAbsolutePath()), fileProcessor);

					System.out.println("done");
				}
			}catch(IOException ioe){
			System.out.println(ioe);
				ordinaryFiles = null;
				directories = null;
				children = null;
				lastmodified = null;
			}
		}

		public CachedFile(String filename, CachedFile overtakeCache){
			super(filename);

			this.ordinaryFiles = overtakeCache.ordinaryFiles;
			this.directories = overtakeCache.directories;
			this.children = overtakeCache.children;
			this.lastmodified = overtakeCache.lastmodified;
		}
		
		public boolean exists(){
			if (ordinaryFiles != null && directories != null){
				return (ordinaryFiles.contains(getAbsolutePath()) || directories.contains(getAbsolutePath()));
			}else{
				return super.exists();
			}
		}
		
		public String[] list(){
			LinkedList<String> curchildren = children.get(getAbsolutePath());
			if (curchildren != null){
				return curchildren.toArray(new String[curchildren.size()]);
			}
			return null;
		}
		
		public boolean isFile(){
			if (directories != null && directories.contains(getAbsolutePath())) return false;
			if (ordinaryFiles != null && ordinaryFiles.contains(getAbsolutePath())) return true;
			return super.isFile();
		}
		
		public boolean isDirectory(){
			if (directories != null && directories.contains(getAbsolutePath())) return true;
			if (ordinaryFiles != null && ordinaryFiles.contains(getAbsolutePath())) return false;
			return super.isDirectory();
		}
		
		public long lastModified(){
			if (lastmodified != null && lastmodified.containsKey(getAbsolutePath())) return lastmodified.get(getAbsolutePath());
			return super.lastModified();
		}
	}

	/**
	 * Collections sync actions for the locations defined in a sync file. The actions are not actually performed yet.
	 * @param syncFile
	 * @param syncdate
	 * @return List<Action> Sync actions planned to perform
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<ActionGroup> synchronize(String syncFile, Date referenceDate) throws Exception{		
		long startTime = System.currentTimeMillis();
		try {
			// Read sync file
			prop = new MultiProperties();
			prop.load(new FileInputStream(syncFile));
			
			// Read sync prefix
			String prefix1 = (prop.containsKey("prefix1") ? prop.getProperty("prefix1") : "").replace("\\", File.separator).replace("/", File.separator) + File.separator;
			String prefix2 = (prop.containsKey("prefix2") ? prop.getProperty("prefix2") : "").replace("\\", File.separator).replace("/", File.separator) + File.separator;

			// Use cache (in case)
			boolean useCache = prop.containsKey("usecache") && new Boolean(prop.getProperty("usecache")).booleanValue();
		
			// Read time margin
			int timemargin = 0;
			if (prop.containsKey("timemargin")) timemargin = Integer.parseInt(prop.getProperty("timemargin"));
			
			// Read sync locations
			List<String> locations = (List<String>)prop.getAsList("synclocations");
			
			// Read sync filters
			List<String> filter = new ArrayList<String>();
			if (prop.containsKey("filterfile")){
				MultiProperties filterprop = new MultiProperties();
				filterprop.load(new FileInputStream(prop.getProperty("filterfile")));
				if (filterprop.containsKey("filter")) filter = (List<String>)filterprop.getAsList("filter");
			}else if (prop.containsKey("filter")){
				filter = (List<String>)prop.getAsList("filter");
			}

			// Synchronize all directories and collect file system actions
			List<ActionGroup> actiongroups = new ArrayList<ActionGroup>();
			for (String currentLocation : locations){
			
				// locations and type of synchronization
				boolean bidirectional = false;
				String loc1 = "";
				String loc2 = "";
				if (currentLocation.contains("<-->")){
					String[] sublocations = currentLocation.split("<-->");
					bidirectional = true;
					loc1 = (sublocations[0].startsWith("!") ? sublocations[0].substring(1) : prefix1 + sublocations[0]);
					loc2 = (sublocations[1].startsWith("!") ? sublocations[1].substring(1) : prefix2 + sublocations[1]);
				}else if (currentLocation.contains("-->")){
					String[] sublocations = currentLocation.split("-->");
					bidirectional = false;
					loc1 = (sublocations[0].startsWith("!") ? sublocations[0].substring(1) : prefix1 + sublocations[0]);
					loc2 = (sublocations[1].startsWith("!") ? sublocations[1].substring(1) : prefix2 + sublocations[1]);
				}else if (currentLocation.contains("<--")){
					String[] sublocations = currentLocation.split("<--");
					bidirectional = false;
					loc1 = (sublocations[0].startsWith("!") ? sublocations[0].substring(1) : prefix1 + sublocations[1]);
					loc2 = (sublocations[1].startsWith("!") ? sublocations[1].substring(1) : prefix2 + sublocations[0]);
				}else{
					// Error
					continue;
				}

				if ((!(new File(loc1).exists())) || (!(new File(loc2).exists()))){
					String rd = (!(new File(loc1).exists())) ? loc1 : loc2;
					throw new Exception("Error: One of the root sync directories (" + rd + ") could not be accessed. Make sure that all devices are correctly mounted.");
				}else{
					// Synchronize locations
					System.out.println("   Analyzing " + loc1 + (bidirectional ? " <--> " : " --> ") + loc2);
					if (useCache){
						actiongroups.addAll(synchronize(	new CachedFile(loc1),
															new CachedFile(loc2),
															referenceDate, bidirectional, filter, timemargin));
					}else{
						actiongroups.addAll(synchronize(	new File(loc1),
															new File(loc2),
															referenceDate, bidirectional, filter, timemargin));
					}
				}
			}
			syncTime = System.currentTimeMillis() - startTime;

			return actiongroups;
		} catch (FileNotFoundException e) {
			throw new Exception("Error: sync config file could not be found");
		} catch (IOException e) {
			throw new Exception("Error: " + e.getMessage());
		}
	}

	/**
	 * Synchronizes two directories. The method does not apply any file operations. Rather it generates a list of suggested modifications.
	 * @param location1
	 * @param location2
	 * @param referenceDate (should be the date of the last import)
	 * @param bidirectional If false, synchronization will only occur from location1 to location2
	 * @throws IllegalArgumentException If one of the locations is a subpath of the other one
	 */
	public List<ActionGroup> synchronize(String loc1, String loc2, boolean bidirectional, Date referencedate) throws Exception{	
		// Synchronize locations
		long startTime = System.currentTimeMillis();
		List<ActionGroup> actiongroups = synchronize(	new File(loc1),
														new File(loc2),
														referencedate, bidirectional, null, 0);
		syncTime = System.currentTimeMillis() - startTime;
		return actiongroups;
	}
	
	/**
	 * Synchronizes two directories. The method does not apply any file operations. Rather it generates a list of suggested modifications.
	 * @param location1
	 * @param location2
	 * @param referenceDate (should be the date of the last import)
	 * @param bidirectional If false, synchronization will only occur from location1 to location2
	 * @param filter
	 * @param timemargin The maximum number of seconds between two timestamps to be treated as equal
	 * @return List<ActionGroup> List of suggested file operations
	 * @author Christoph Redl
	 * @throws IllegalArgumentException If one of the locations is a subpath of the other one
	 */
	public List<ActionGroup> synchronize(File location1, File location2, Date referenceDate, boolean bidirectional, List<String> filter, int timemargin) throws IllegalArgumentException{
		// Sanity check
		if (location1.getAbsolutePath().startsWith(location2.getAbsolutePath()) ||
			location2.getAbsolutePath().startsWith(location1.getAbsolutePath())){
			throw new IllegalArgumentException("One of the locations is subpath of the other one");
		}
	
		List<ActionGroup> actions = new ArrayList<ActionGroup>(); 

		// Check if both locations dir1 and dir2 exist
		if (location1.exists() && location2.exists()){
			// Filter
			if (isFilterAppicable("Name:" + location1.getName(), filter)) return actions;
			if (isFilterAppicable("Path:" + location1.getAbsolutePath().replace(File.separator, "/"), filter)) return actions;
			if (isFilterAppicable("Name:" + location2.getName(), filter)) return actions;
			if (isFilterAppicable("Path:" + location2.getAbsolutePath().replace(File.separator, "/"), filter)) return actions;

			// ignore symbolic links
			boolean ignoreSymbolicLinks = !prop.containsKey("ignoresymboliclinks") || new Boolean(prop.getProperty("ignoresymboliclinks")).booleanValue();
			if (ignoreSymbolicLinks){
				if (java.nio.file.Files.isSymbolicLink(location1.toPath()) || java.nio.file.Files.isSymbolicLink(location2.toPath())) return actions;
			}

			// Files or directories?
			if (location1.isFile() && location2.isFile()){
				// Files
				// Compare files
				if (Math.abs((location1.lastModified() / 1000) - (location2.lastModified() / 1000)) > timemargin){
					// If both copies were modified after the last synchronization, there is a conflict
					if (location1.lastModified() > referenceDate.getTime() && location2.lastModified() > referenceDate.getTime()){
						ActionGroup ag = new ActionGroup("Conflict", '>', '<',
								new Action(Action.Type.CopyFile, location1.getAbsolutePath(), location2.getAbsolutePath()),
								new Action(Action.Type.CopyFile, location2.getAbsolutePath(), location1.getAbsolutePath()));
						ag.unselectAction();
						actions.add(ag);
					}else{
						// Files are different: Check which one is more recent
						if (location1.lastModified() < location2.lastModified()){
							// location2 is newer
							if (bidirectional){
								actions.add(new ActionGroup('<', '>',
										new Action(Action.Type.CopyFile, location2.getAbsolutePath(), location1.getAbsolutePath()),
										new Action(Action.Type.CopyFile, location1.getAbsolutePath(), location2.getAbsolutePath())
								));
							}
						}else{
							// location1 is newer
							actions.add(new ActionGroup('>', '<',
									new Action(Action.Type.CopyFile, location1.getAbsolutePath(), location2.getAbsolutePath()),
									new Action(Action.Type.CopyFile, location2.getAbsolutePath(), location1.getAbsolutePath())
							));
						}
					}
				}
			}else if(location1.isDirectory() && location2.isDirectory()){
				// Directories
				
				// Make a list of all subdirecories
				String[] children1 = location1.list();
				String[] children2 = location2.list();
				Set<String> children = new TreeSet<String>();
				if (children1 != null){
					for (int i = 0; i < children1.length; i++){
						if (children1[i].compareTo("") != 0) children.add(children1[i]);
					}
				}
				if (children2 != null){
					for (int i = 0; i < children2.length; i++){
						if (children2[i].compareTo("") != 0) children.add(children2[i]);
					}
				}
				
				// Recursive traversal through the subdirectories
				for (String child : children){
					if (location1 instanceof CachedFile && location2 instanceof CachedFile){
						actions.addAll(synchronize(new CachedFile(location1.getAbsolutePath() + File.separator + child, (CachedFile)location1), new CachedFile(location2.getAbsolutePath() + File.separator + child, (CachedFile)location2), referenceDate, bidirectional, filter, timemargin));
					}else{
						actions.addAll(synchronize(new File(location1.getAbsolutePath() + File.separator + child), new File(location2.getAbsolutePath() + File.separator + child), referenceDate, bidirectional, filter, timemargin));
					}
				}
			}else{
				// Error: One of the locations is a file, the other one is a directory
				ActionGroup ag = new ActionGroup(	"Conflict", '>', '<',
													new Action(location1.isDirectory() ? Action.Type.DelAndCopyDirectory : Action.Type.DelAndCopyFile, location1.getAbsolutePath(), location2.getAbsolutePath()),
													new Action(location2.isDirectory() ? Action.Type.DelAndCopyDirectory : Action.Type.DelAndCopyFile, location2.getAbsolutePath(), location1.getAbsolutePath()));
				ag.unselectAction();
				actions.add(ag);
			}
		}else if (!location1.exists() && !location2.exists()){
			// None of them exists --> nothing to do (should not happen, is only possible if configuration is wrong)
			return actions;
		}else{
			// Check if location1 or location2 is missing
			if (location1.exists()){
				// Filter
				if (isFilterAppicable("Name:" + location1.getName(), filter)) return actions;
				if (isFilterAppicable("Path:" + location1.getAbsolutePath().replace(File.separator, "/"), filter)) return actions;

				// location2 is missing
				// Check if location1 and its parent modification date are both older then the reference date (parent is considered because rename events do not modify the date of the element itself)
				// if this is the case, location1 should be deleted since the copy without the element is more recent; otherwise location1 should be copied to location2
				if (new Date(location1.lastModified()).compareTo(referenceDate) < 0 && new Date(location1.getParentFile().lastModified()).compareTo(referenceDate) < 0){
					if (bidirectional){
						actions.add(new ActionGroup('<', '>',
								// Primary action is: delete from location1
								new Action(location1.isFile() ? Action.Type.DeleteFile : Action.Type.DeleteDirectory, location1.getAbsolutePath()),
								// Alternative is: copy location1 to location2
								new Action(location1.isFile() ? Action.Type.CopyFile : Action.Type.CopyDirectory, location1.getAbsolutePath(), location2.getAbsolutePath())
						));
					}
				}else if(new Date(location1.lastModified()).compareTo(referenceDate) >= 0 && new Date(location1.getParentFile().lastModified()).compareTo(referenceDate) >= 0){
					actions.add(new ActionGroup('>', '<',
							// Primary action is: copy location1 to location2
							new Action(location1.isFile() ? Action.Type.CopyFile : Action.Type.CopyDirectory, location1.getAbsolutePath(), location2.getAbsolutePath()),
							// Alternative is: delete from location1
							new Action(location1.isFile() ? Action.Type.DeleteFile : Action.Type.DeleteDirectory, location1.getAbsolutePath())
					));
				}else{
					actions.add(new ActionGroup("(please check)", '>', '<',
							// Primary action is: copy location1 to location2
							new Action(location1.isFile() ? Action.Type.CopyFile : Action.Type.CopyDirectory, location1.getAbsolutePath(), location2.getAbsolutePath()),
							// Alternative is: delete from location1
							new Action(location1.isFile() ? Action.Type.DeleteFile : Action.Type.DeleteDirectory, location1.getAbsolutePath())
					));
				}
			}else{
				// Filter
				if (isFilterAppicable("Name:" + location2.getName(), filter)) return actions;
				if (isFilterAppicable("Path:" + location2.getAbsolutePath().replace(File.separator, "/"), filter)) return actions;

				// location1 is missing
				// Check if location2 and its parent are both older then the reference date (parent is considered because rename events do not modify the date of the element itself)
				// if this is the case, location2 should be deleted since the copy without the element is more recent; otherwise location2 should be copied to location1
				if (new Date(location2.lastModified()).compareTo(referenceDate) < 0 && new Date(location2.getParentFile().lastModified()).compareTo(referenceDate) < 0){
					if (bidirectional){
						actions.add(new ActionGroup('>', '<',
								// Primary action is: delete from location2
								new Action(location2.isFile() ? Action.Type.DeleteFile : Action.Type.DeleteDirectory, location2.getAbsolutePath()),
								// Alternative is: copy location2 to location1
								new Action(location2.isFile() ? Action.Type.CopyFile : Action.Type.CopyDirectory, location2.getAbsolutePath(), location1.getAbsolutePath())
						));
					}
				}else if(new Date(location2.lastModified()).compareTo(referenceDate) >= 0 && new Date(location2.getParentFile().lastModified()).compareTo(referenceDate) >= 0){
					actions.add(new ActionGroup('<', '>',
							// Primary action is: copy location2 to location1
							new Action(location2.isFile() ? Action.Type.CopyFile : Action.Type.CopyDirectory, location2.getAbsolutePath(), location1.getAbsolutePath()),
							// Alternative is: delete from location2
							new Action(location2.isFile() ? Action.Type.DeleteFile : Action.Type.DeleteDirectory, location2.getAbsolutePath())
					));
				}else{
					actions.add(new ActionGroup("(please check)", '<', '>',
							// Primary action is: copy location2 to location1
							new Action(location2.isFile() ? Action.Type.CopyFile : Action.Type.CopyDirectory, location2.getAbsolutePath(), location1.getAbsolutePath()),
							// Alternative is: delete from location2
							new Action(location2.isFile() ? Action.Type.DeleteFile : Action.Type.DeleteDirectory, location2.getAbsolutePath())
					));
				}
			}
		}
		
		return actions;
	}
	
	/**
	 * Checks if one of the filters is applicable to a given filterstring
	 * @param filterstring
	 * @param filterlist
	 * @return boolean
	 */
	private static boolean isFilterAppicable(String filterstring, List<String> filterlist){
		if (filterlist == null) return false;
		for (String filter : filterlist){
			Pattern p = Pattern.compile(filter);
			Matcher m = p.matcher(filterstring);
			if (m.matches()) return true;
		}
		return false;
	}
	
	/**
	 * Returns the synchronization time in seconds (only valid after success).
	 * @return Synchronization time in seconds
	 */
	public long getSyncTime(){
		return syncTime / 1000;
	}
}
