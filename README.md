dsync Introduction
==================

dsync is a utility for two-way synchronization of directories.
Synchronization is based on comparing file modification dates to each other
and (possibly) to a reference date, which is usually the date of the last synchronization.
Directories are recursively processed.

For each detected difference (files which exist at one location and not at the other
or which have different contents), the utility will provide a list with all
possibilities to resolve the conflict, but will select the default alternative which is more
likly to be intended by the user. Actions are called either ">" or "<" to resemble
the intuitive meaning of mirroring the first location to the second (">") one or vice versa ("<").

More specifically, if a file exists at both locations with a different content,
the alternatives are to copy it in the one or the other direction. If one of the files
was modified before and the other one after the reference date, the newer file is
more likely to be the intended one and thus copying it to overwrite the older one
is the default alternative. If a file is present at one location but missing at the other one,
then the default selection is to copy it to the one where it is missing if the modification
date is after the reference date (the file was probably created in the meantime),
while the default is to delete it if the modification date is before the reference date
(the file was probably deleted since the previous synchronization).
In all other cases (e.g. the files differ and both modification dates are after the reference date,
i.e., both copies were modified since the last synchronization),
a conflict is discovered and there is no default selection.

After analyzing the directories but before executing the actions, dsync provides a list
of actions planned to be executed. For each action, it shows the alternatives
and the user is enabled to switch to other alternatives.
For elements without default selection (dsync was not able to derive the intended modification
due to a conflict), the user should make a selection; otherwise no action is performed on this element.
Alternatives are selected by entering commands such as ">50-100" in order to select the action ">"
for all elements from index 50 to 100, i.e., modifying the second location to the status of
the first one. After (possibly) modifying the list, the command "ok" executes it.


Parameters
==========

The utility can be called by executing:

   java -jar dsync.jar [PARAMETERS]

There are the following possibilities for the parameters:

   - two locations (directories to be synchronized)
   - two locations and a bidirectional flag (true/false);
        unidirectional synchronization will never modify the first location
   - two locations, bidirectional flag and reference date
   - a sync file (see below)

Default values (for missing parameters in the first two alternatives) are: bidirectional, reference date is January 1, 1970, 01:00:00 GMT


Sync files
==========

While passing the locations directly as parameters, a sync file is much more flexible and allows for specifying multiple directories to be synchronized.
The utility is then called with:

   java -jar dsync.jar syncfile.txt

A sync file has the following format:

   synclocations=d1<-->d2;d3-->d4;d5<--d6
   prefix1=/home/user
   prefix2=/home/user
   filter=Name:.*;;Path:.*
   filterfile=[path]
   referencedate=01.01.2000
   lastsync=0
   lastsyncfile=[path]
   usecaching=[boolean]
   ignoresymboliclinks=[boolean]

Only one of "referencedate", "lastsync" or "lastsyncfile" must be specified (usually "lastsyncfile").
   
   
Description of the elements:

   - synclocations:             Semicolon-separated list of files and directories to be synchronized (relative to prefix1 and prefix2).
                                   The arrow specifies if the directories are synchronized in one or both directions (<--, --> or <-->).
   - prefix1/prefix2:           Path to be used as a prefix for all elements in "synclocations".
   - filter:                    Double-semicolon separated list of filename or path patterns to be skipped
                                   (asterisks as a joker, prefixed with "Name:" or "Path:" to specify whether it applies to filename or full paths).
   - filefile:                  Name of a file of the form "filter=[DEFINITION]", where "[DEFINITION]" is as in "filter".
   - referencedate/lastsync     Date to compare to ("referencedate" if in human- and "lastsync" if in machine-readable format)
        lastsyncfile:              or a file to contain this date in machine-readable form and which is updated after the synchronization ("lastsyncfile"); normally, "lastsyncfile" will be used.
   - usecaching:                Use a cache while analyzing directoriy structures; this might be faster depending on the operating and file system, especially for remove directories.
   - ignoresymboliclinks:       Do not synchronize symbolic links.

Note: By default, the reference date is the last synchronization date. This can be overwritten by specifying attribute 'referencedate'.


Synchronizing with remove locations
===================================

Synchronizing local and remote directories is useful e.g. for synchronizing multiple devices.
There are several possibilities to do this. The dsync utility is rather independent of the choice of a certain alternative as long as the paths point to the local or remote directories.

One alternative under Linux systems is to mount remove directories locally using using the cifs file system (Samba).
For mounting using WINS names (rather than IP addresses), the package winbind nees to be installed.
Depending on the distribution this requires a command such as (on Ubuntu):

   apt-get install winbind

Make sure that "/etc/nsswitch.conf" has "wins" in the "hosts" line, as in:

   hosts: files dns wins

Besides the actual call to dsync, synchronization requires then to mount the remote directory before and to unmount it after the call:

   sudo mount -o user=[SAMBAUSER] -t cifs //[REMOVEHOST]/[REMOEVDIRECTORY] [MOUNTDIRECTORY]
   java -jar dsync.jar syncfile.txt
   sudo umount [MOUNTDIRECTORY]

However, there are also several other options to work with remove directories.
For instance, under Windows systems the paths might directly point to the remove location (e.g. "\\my-remote-computer\data").


Author
======

Christoph Redl (redl@kr.tuwien.ac.at)
