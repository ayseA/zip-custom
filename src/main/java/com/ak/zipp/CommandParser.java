package com.ak.zipp;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandParser {
	
	private static final Set<String> allSwitches = Set.copyOf(
			Stream.concat(
					Switch.allNamesDashed().stream(), 
					Switch.allShortNamesDashed().stream())
			.collect(Collectors.toSet()));

	public static EnumMap<Switch, Set<String>> processCommand(String[] fullCmd) {

		// not even a zipp command
		if (fullCmd==null || fullCmd.length<1) {
			System.out.println("Nothing to execute. Returning as is. ");
			return null;
		}

		if (!fullCmd[0].equalsIgnoreCase("zipp")) {
			System.out.println("this isn't a zipp command. Returning as is. ");
			return null;
		}

		// the cmd is "zipp". 
		if (fullCmd.length==1)
			return setFilterDefaults(new EnumMap<>(Switch.class));

		// give a good starter to parseCommands()
		if (!allSwitches.contains(fullCmd[1].toUpperCase()))
			throw new IllegalArgumentException("Invalid switch ["
					+ fullCmd[1]
					+ "] must be one of "
					+Switch.allNamesDashed()
					+" or of "+Switch.allShortNamesDashed());
		
		EnumMap<Switch, Set<String>> switches = parseCommands(Arrays.copyOfRange(fullCmd, 1, fullCmd.length));
		return setFilterDefaults(switches);
	}

	private static EnumMap<Switch, Set<String>> parseCommands(String[] fullCmd) {
		EnumMap<Switch, Set<String>> parsedCommands = new EnumMap<>(Switch.class);
		Switch currSwitch = null; 
		Set<String> theSet = null; 
		for (String part : fullCmd) 
			if (allSwitches.contains(part.toUpperCase())) {
				if (parsedCommands.keySet().contains(Switch.correspondingSwitch(part)))
					throw new IllegalArgumentException("Duplicate use of switch: "+part);
				if (currSwitch!=null 
						&& !currSwitch.equals(Switch.NORECURSE)  // Switch.NORECURSE: the only no-argument Switch
						&& theSet.isEmpty())
					parsedCommands.remove(currSwitch);
				parsedCommands.put(currSwitch=Switch.correspondingSwitch(part), theSet=new HashSet<>());
			} else {
				if (part.startsWith("-"))
					if (!allSwitches.contains(fullCmd[1].toUpperCase()))
						throw new IllegalArgumentException("Invalid switch ["
								+ part
								+ "] must be one of "
								+Switch.allNamesDashed()
								+" or of "+Switch.allShortNamesDashed());
				if (currSwitch.equals(Switch.NORECURSE))
					throw new IllegalArgumentException("The switch -noRecurse does NOT take any arguments"); 
				if (!currSwitch.isMultiValued() && theSet.size()>0)
					throw new IllegalArgumentException("Invalid argument ["+part+ "] -- switch "+currSwitch+" can NOT take multiple arguments");
				if ( (part.contains(File.separator) // arguments of multi-valued switches and Switch.ZIPFILE can only be filenames-- no path infp in the filename
						|| part.equals(".") || part.equals(".."))  // current/parent dir not allowed in arguments multi-valued switches or Switch.ZIPFILE 
						&& (currSwitch.isMultiValued() 
								|| currSwitch.equals(Switch.ZIPFILE)))
					throw new IllegalArgumentException("Invalid argument "+part
							+ " -- the switch "+currSwitch+" takes file names without the path info");
				theSet.add(part);
			}
		return parsedCommands;
	}

	private static EnumMap<Switch, Set<String>> setFilterDefaults(EnumMap<Switch, Set<String>> cmds) {
		Set<String> tmp; 

		// set defaults for *INCLUDE
		if ((tmp=cmds.get(Switch.INCLUDE))==null 
				|| tmp.isEmpty()) 
			cmds.put(Switch.INCLUDE, Set.of("*"));
		if ( !cmds.containsKey(Switch.NORECURSE) 
				&& ((tmp=cmds.get(Switch.DEEPINCLUDE))==null 
					|| tmp.isEmpty()) ) 
			cmds.put(Switch.DEEPINCLUDE, Set.of("*"));

		// set defaults for *EXCLUDE
		if ((tmp=cmds.get(Switch.EXCLUDE))==null)
			cmds.put(Switch.EXCLUDE, Set.of());
		if ( !cmds.containsKey(Switch.NORECURSE) 
				&& (tmp=cmds.get(Switch.DEEPEXCLUDE))==null ) 
			cmds.put(Switch.DEEPEXCLUDE, Set.of());

		return cmds; 
	}

}
