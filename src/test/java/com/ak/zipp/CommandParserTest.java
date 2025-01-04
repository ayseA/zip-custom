package com.ak.zipp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.EnumMap;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CommandParserTest {

    @Test
    void testProcessCommand_nullInput() {
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(null);
        assertNull(result, "Command should return null for null input");
    }

    @Test
    void testProcessCommand_emptyInput() {
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(new String[]{});
        assertNull(result, "Command should return null for empty input");
    }

    @Test
    void testProcessCommand_notZippCommand() {
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(new String[]{"invalid"});
        assertNull(result, "Command should return null if it's not a 'zipp' command");
    }

    @Test
    void testProcessCommand_invalidSwitch() {
        assertThrows(IllegalArgumentException.class, () -> {
            CommandParser.processCommand(new String[]{"zipp", "-invalid"});
        }, "Invalid switch should throw IllegalArgumentException");
    }

    @Test
    void testProcessCommand_switchWithNoValues() {
        String[] cmd = new String[]{"zipp", "-srcDir", "-include"};
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(cmd);
        assertNotNull(result);
        assertTrue(!result.containsKey(Switch.SRCDIR), "Map should not contain SRCDIR switch when no arguments for it");
    }

    @Test
    void testProcessCommand_switchWithSingleOrNoArguments() {
    	assertThrows(IllegalArgumentException.class, () -> {
    		CommandParser.processCommand(new String[]{"zipp", "-srcDir", "folder1", "-folder2"});
    	}, "Multiple values in a single-valued switch should throw IllegalArgumentException");
    	assertThrows(IllegalArgumentException.class, () -> {
    		CommandParser.processCommand(new String[]{"zipp", "-noRecurse", "someValue"});
    	}, "A Values in a "+ Switch.NORECURSE +" switch should throw IllegalArgumentException");
    }

    @Test
    void testProcessCommand_switchWithMultipleArguments() {
        String[] cmd = new String[]{"zipp", "-include", "file1.txt", "file2.txt"};
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(cmd);
        assertNotNull(result, "Command should return a map for valid switches");
        assertTrue(result.containsKey(Switch.INCLUDE), "Map should contain INCLUDE switch");
        assertTrue(result.get(Switch.INCLUDE).contains("file1.txt") 
        		&& result.get(Switch.INCLUDE).contains("file2.txt")
        		, "Map should contain correct file argument");

        cmd = new String[]{"zipp", "-include", "-de"};
        result = CommandParser.processCommand(cmd);
        assertNotNull(result, "Command should return a map for valid switches");
        assertTrue(result.containsKey(Switch.INCLUDE), "Map should contain INCLUDE switch");
        assertTrue(result.get(Switch.INCLUDE).contains("*") 
        		&& result.get(Switch.INCLUDE).size()==1
        		, "Map should contain correct file arguments");
        assertTrue(result.containsKey(Switch.DEEPEXCLUDE), "Map should contain DEEPEXCLUDE switch");
        assertTrue(result.get(Switch.DEEPEXCLUDE).isEmpty()
        		, "Map should contain correct file arguments");
}

    @Test
    void testProcessCommand_duplicateSwitch() {
        assertThrows(IllegalArgumentException.class, () -> {
            CommandParser.processCommand(new String[]{"zipp", "-include", "file1.txt", "-include", "file2.txt"});
        }, "Duplicate switch should throw IllegalArgumentException");
    }

    @Test
    void testProcessCommand_switchWithArguments() {
        String[] cmd = new String[]{"zipp", "-include", "file1.txt", "-exclude", "file2.txt"};
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(cmd);
        assertNotNull(result, "Command should return a map for valid switches");
        assertTrue(result.containsKey(Switch.INCLUDE), "Map should contain INCLUDE switch");
        assertTrue(result.get(Switch.INCLUDE).contains("file1.txt"), "Map should contain correct file argument for INCLUDE");
        assertTrue(result.containsKey(Switch.EXCLUDE), "Map should contain EXCLUDE switch");
        assertTrue(result.get(Switch.EXCLUDE).contains("file2.txt"), "Map should contain correct file argument for EXCLUDE");
    }

    @Test
    void testProcessCommand_invalidFileWithPath() {
        assertThrows(IllegalArgumentException.class, () -> CommandParser.processCommand(new String[]{
                "zipp", "-zipFile", "C:" + File.separator + "somePath" + File.separator + "file.zip"}), 
                "Invalid file path should throw IllegalArgumentException for ZIPFILE switch");

        assertThrows(IllegalArgumentException.class, () 
        		-> CommandParser.processCommand(new String[]{"zipp", "-de", "."}));
        assertThrows(IllegalArgumentException.class, () 
        		-> CommandParser.processCommand(new String[]{"zipp", "-z", ".."}));

    }

    @Test
    void testProcessCommand_defaultIncludeExclude() {
        String[] cmd = new String[]{"zipp"};
        EnumMap<Switch, Set<String>> result = CommandParser.processCommand(cmd);
        assertNotNull(result, "Command should return a map with default INCLUDE and EXCLUDE");
        assertTrue(result.containsKey(Switch.INCLUDE), "Map should contain INCLUDE switch");
        assertTrue(result.get(Switch.INCLUDE).contains("*"), "INCLUDE should default to '*' if not provided");
        assertTrue(result.containsKey(Switch.EXCLUDE), "Map should contain EXCLUDE switch");
        assertTrue(result.get(Switch.EXCLUDE).isEmpty(), "EXCLUDE should default to empty set");
    }
}
