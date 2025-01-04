package com.ak.zipp;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum Switch {

	SRCDIR, DSTDIR, // A, B, 
	INCLUDE, EXCLUDE, // C, D, 
	DEEPINCLUDE, DEEPEXCLUDE,  // E, F, 
	NORECURSE,  // G
	ZIPFILE;  // H	
	
	private static final Set<String> switchesDashed = Set.of(Switch.values()).stream()
			.map(s -> "-" + s.name())
			.collect(Collectors.toSet());
	public static Set<String> allNamesDashed(){
		return switchesDashed;
	}

	private static final Map<String, String> shortNamesMap = 
			Arrays.stream(Switch.values())
			.collect(Collectors.toMap(
					Enum::name,
					s -> {
						String name = s.name().toUpperCase();
						return switch (name) {
						case "DEEPINCLUDE" -> "DI";
						case "DEEPEXCLUDE" -> "DE";
						case "NORECURSE" -> "NR";
						default -> name.substring(0, 1);
						};
					}
					));
	
	private static final Set<String> shortNamesDashed = shortNamesMap.values().stream()
			.map(v -> "-" + v)
			.collect(Collectors.toSet());
	public static Set<String> allShortNamesDashed() {
		return Set.copyOf(shortNamesDashed);
	}
	public String shortNameDashed() {
		return "-"+shortNamesMap.get(this.name());
	}
	public String nameDashed() {
		return "-"+this.name();
	}

	/**
	 * returns the Switch whose dashed-name or dashed-shorname 
	 * is the passed-in param
	 * 
	 * @return
	 */
	public static Switch correspondingSwitch(String str) {
		for (Switch s : Switch.values()) 
			if (s.nameDashed().equalsIgnoreCase(str)
					|| s.shortNameDashed().equalsIgnoreCase(str)) 
				return s;
		return null;
	}

	public boolean isMultiValued() {  // hard-coding of a kind. but don't mind. 
		if (this.name().endsWith("CLUDE"))
			return true; 
		return false; 
	}

}