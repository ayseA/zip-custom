package com.ak.zipp;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class SwitchTest {

    @Test
    void testAllNamesDashed() {
        Set<String> expected = Set.of("-SRCDIR", "-DSTDIR", "-INCLUDE", "-EXCLUDE", "-DEEPINCLUDE", "-DEEPEXCLUDE", "-NORECURSE", "-ZIPFILE");
        assertEquals(expected, Switch.allNamesDashed());
    }

    @Test
    void testAllShortNamesDashed() {
        Set<String> expected = Set.of("-S", "-D", "-I", "-E", "-DI", "-DE", "-NR", "-Z");
        assertEquals(expected, Switch.allShortNamesDashed());
    }

    @Test
    void testShortNameDashed() {
        assertEquals("-S", Switch.SRCDIR.shortNameDashed());
        assertEquals("-DI", Switch.DEEPINCLUDE.shortNameDashed());
    }

    @Test
    void testNameDashed() {
        assertEquals("-SRCDIR", Switch.SRCDIR.nameDashed());
        assertEquals("-DEEPINCLUDE", Switch.DEEPINCLUDE.nameDashed());
    }

    @Test
    void testCorrespondingSwitch() {
        assertEquals(Switch.SRCDIR, Switch.correspondingSwitch("-SRCDIR"));
        assertEquals(Switch.DEEPINCLUDE, Switch.correspondingSwitch("-DI"));
        assertNull(Switch.correspondingSwitch("-UNKNOWN"));
    }

    @Test
    void testIsMultiValued() {
        assertTrue(Switch.INCLUDE.isMultiValued());
        assertFalse(Switch.SRCDIR.isMultiValued());
    }
}