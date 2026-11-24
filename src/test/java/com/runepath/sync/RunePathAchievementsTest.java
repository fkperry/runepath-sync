package com.runepath.sync;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class RunePathAchievementsTest
{
    @Test
    public void combatTaskBitFlagsHandleLowAndHighBits()
    {
        assertTrue(RunePathAchievements.taskComplete(0, 1));
        assertTrue(RunePathAchievements.taskComplete(31, Integer.MIN_VALUE));
        assertTrue(RunePathAchievements.taskComplete(63, Integer.MIN_VALUE));
        assertFalse(RunePathAchievements.taskComplete(30, Integer.MIN_VALUE));
        assertFalse(RunePathAchievements.taskComplete(1, 1));
    }

    @Test
    public void combatCatalogFitsBitsetAndContainsUniqueStructures() throws Exception
    {
        int[] ids = RunePathAchievements.COMBAT_STRUCT_IDS;
        int[] varps = RunePathAchievements.COMBAT_TASK_VARPS;
        assertEquals("Validate game cache task catalog when OSRS updates", 637, ids.length);
        assertEquals("Bit-packed varps should cover all indexed tasks", 20, varps.length);
        assertTrue(ids.length <= varps.length * 32);
        Set<Integer> unique = new HashSet<>();
        for (int id : ids) assertTrue("Duplicate combat cache struct: " + id, unique.add(id));
    }

    @Test
    public void diariesHaveTwelveAreasAndFourTiersEach() throws Exception
    {
        String[] names = RunePathAchievements.DIARY_AREAS;
        int[][] completion = RunePathAchievements.DIARY_COMPLETE_VARBITS;
        int[][] counts = RunePathAchievements.DIARY_COUNT_VARBITS;
        assertEquals(12, names.length);
        assertEquals(names.length, completion.length);
        assertEquals(names.length, counts.length);
        for (int i = 0; i < names.length; i++)
        {
            assertEquals("Completion flags: " + names[i], 4, completion[i].length);
            assertEquals("Task counts: " + names[i], 4, counts[i].length);
        }
    }

}
