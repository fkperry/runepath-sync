package com.runepath.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.StructComposition;
import net.runelite.api.widgets.Widget;

/**
 * Capture achievement progression on the RuneLite client thread.
 *
 * Source data:
 * - completion flags and task counts are game varbits;
 * - combat task IDs/names/tier metadata are read from game cache structs;
 * - combat task completion flags are bit-packed into varps;
 * - diary task names are opportunistically observed from the opened journal.
 *
 * The game can change these identifiers between releases. If combat task
 * metadata cannot be loaded completely, omit that dataset instead of
 * overwriting a known-good cloud snapshot with false incompletes.
 */
final class RunePathAchievements
{
    static final String[] DIARY_AREAS = {"Ardougne", "Desert", "Falador", "Fremennik", "Kandarin", "Karamja", "Kourend & Kebos", "Lumbridge & Draynor", "Morytania", "Varrock", "Western Provinces", "Wilderness"};
    private static final String[] DIARY_TIERS = {"easy", "medium", "hard", "elite"};
    static final int[][] DIARY_COMPLETE_VARBITS = {
        {4458, 4459, 4460, 4461},
        {4483, 4484, 4485, 4486},
        {4462, 4463, 4464, 4465},
        {4491, 4492, 4493, 4494},
        {4475, 4476, 4477, 4478},
        {3578, 3599, 3611, 4566},
        {7925, 7926, 7927, 7928},
        {4495, 4496, 4497, 4498},
        {4487, 4488, 4489, 4490},
        {4479, 4480, 4481, 4482},
        {4471, 4472, 4473, 4474},
        {4466, 4467, 4468, 4469}
    };
    static final int[][] DIARY_COUNT_VARBITS = {
        {6291, 6292, 6293, 6294}, 
        {6307, 6308, 6309, 6310}, 
        {6299, 6300, 6301, 6302}, 
        {6323, 6324, 6325, 6326}, 
        {6327, 6328, 6329, 6330}, 
        {2423, 6288, 6289, 6290}, 
        {7933, 7934, 7935, 7936}, 
        {6295, 6296, 6297, 6298}, 
        {6315, 6316, 6317, 6318}, 
        {6303, 6304, 6305, 6306}, 
        {6319, 6320, 6321, 6322}, 
        {6311, 6312, 6313, 6314}, 
    };
    private static final String[] COMBAT_TIERS =
        {"easy", "medium", "hard", "elite", "master", "grandmaster"};
    private static final int[] COMBAT_TIER_FLAGS = {12863, 12864, 12865, 12866, 12867, 12868};
    private static final int[] COMBAT_TIER_COUNTS = {12885, 12886, 12887, 12888, 12889, 12890};
    static final int[] COMBAT_TASK_VARPS = {
        3116, 3117, 3118, 3119, 3120, 3121, 3122, 3123, 3124, 3125,
        3126, 3127, 3128, 3387, 3718, 3773, 3774, 4204, 4496, 4721,
    };
    static final int[] COMBAT_STRUCT_IDS = {
        327, 3164, 3168, 3172, 3173, 3175, 3177, 3179, 3180, 3181,     
        3216, 3220, 3538, 3260, 3261, 3262, 3269, 3271, 3274, 3302,     
        3314, 3600, 3317, 3346, 3419, 3421, 3422, 3425, 3427, 3493,     
        3496, 3497, 3500, 834, 836, 2625, 2628, 3060, 6382, 6387,       
        6408, 3539, 3165, 3166, 3167, 3174, 3176, 3178, 3186, 3188,     
        3201, 3203, 3210, 3212, 3213, 3217, 3218, 3219, 3237, 3270,     
        3272, 3273, 3303, 3304, 3305, 3306, 3313, 3318, 3322, 3337,     
        3341, 3344, 3347, 3350, 3363, 3373, 3420, 3423, 3424, 3184,     
        3494, 3499, 835, 837, 838, 917, 918, 925, 926, 1024,            
        1030, 1035, 1040, 2577, 2623, 3814, 6383, 6384, 6385, 6409,     
        6411, 328, 380, 399, 404, 410, 550, 3156, 3158, 3160,           
        3169, 3170, 3182, 3185, 3197, 3199, 3200, 3202, 3204, 3211,     
        3222, 3224, 3225, 3226, 3227, 3228, 3263, 3264, 3265, 3297,     
        3299, 3307, 3308, 3309, 3310, 3319, 3321, 3323, 3338, 3342,     
        3348, 3349, 3351, 3353, 3358, 3360, 3361, 3364, 3374, 3537,     
        3404, 3406, 3426, 3472, 3474, 3475, 3479, 3495, 3498, 4433,     
        4434, 4436, 4437, 4483, 920, 921, 924, 927, 928, 890,           
        919, 1025, 1028, 1031, 1036, 1037, 1039, 1042, 1043, 2535,      
        2626, 2627, 2630, 6386, 6388, 6412, 366, 388, 400, 405,         
        443, 3157, 3159, 3171, 3183, 3187, 3189, 3191, 3194, 3195,      
        3196, 3198, 3205, 3207, 3208, 3209, 3214, 3215, 3221, 3223,     
        3229, 3230, 3232, 3235, 3238, 3241, 3249, 3251, 3254, 3255,     
        3256, 3259, 3266, 3267, 3275, 3287, 3293, 3298, 3300, 3301,     
        3311, 3316, 3320, 3324, 3327, 3329, 3331, 3334, 3339, 3340,     
        3343, 3345, 3352, 3356, 3359, 3362, 3365, 3367, 3368, 3370,     
        3375, 3376, 3526, 3527, 3528, 3529, 3530, 3531, 3532, 3533,     
        3534, 3535, 3377, 3401, 3402, 3403, 3405, 3407, 3408, 3413,     
        3411, 3432, 3439, 3442, 3443, 3448, 3447, 3451, 3452, 3454,     
        3456, 3460, 3461, 3468, 3469, 3470, 3471, 3473, 3477, 3476,     
        3481, 3289, 3501, 3504, 3507, 304, 4137, 4139, 4435, 4440,      
        4441, 4402, 4403, 4397, 4421, 4423, 4430, 4404, 4407, 4410,     
        4424, 4480, 4484, 4478, 4487, 826, 791, 802, 813, 821,          
        786, 799, 810, 808, 916, 935, 938, 943, 970, 971,               
        972, 974, 1000, 1008, 1002, 1032, 1026, 1027, 1038, 1041,       
        3019, 4739, 4809, 4861, 999, 4558, 4566, 6410, 446, 602,        
        3190, 3192, 3193, 3206, 3231, 3233, 3236, 3239, 3242, 3243,     
        3245, 3247, 3250, 3252, 3253, 3257, 3268, 3276, 3277, 3278,     
        3279, 3280, 3281, 3282, 3284, 3286, 3288, 3290, 3292, 3295,     
        3312, 3325, 3328, 3332, 3335, 3355, 3366, 3369, 3371, 3390,     
        3384, 3380, 3385, 3386, 3382, 3387, 3391, 3388, 3381, 3389,     
        3393, 3395, 3397, 3399, 3378, 3409, 3410, 3412, 3416, 3418,     
        3428, 3430, 3431, 3433, 3435, 3437, 3440, 3444, 3445, 3446,     
        3449, 3450, 3453, 3455, 3457, 3458, 3459, 3462, 3466, 3464,     
        3486, 3502, 3505, 3508, 3509, 3511, 3521, 3536, 922, 940,       
        958, 959, 967, 4138, 4140, 4141, 4142, 4143, 4147, 4396,        
        4438, 4439, 4393, 4419, 4442, 4425, 4427, 4428, 4429, 4431,     
        4432, 4398, 4399, 4400, 4401, 4405, 4408, 4411, 4413, 4414,     
        4416, 4477, 4479, 4481, 4485, 4488, 4486, 807, 827, 792,        
        803, 816, 822, 789, 800, 811, 830, 829, 819, 818,               
        798, 794, 929, 933, 937, 939, 944, 973, 1001, 1003,             
        1005, 1007, 1010, 1011, 1029, 1033, 4740, 4810, 4857, 4859,     
        4860, 4556, 4559, 4560, 4565, 4567, 718, 1690, 3161, 3162,      
        3163, 3234, 3240, 3244, 3246, 3248, 3258, 3283, 3285, 3291,     
        3294, 3296, 3326, 3330, 3333, 3336, 3354, 3357, 3372, 3379,     
        3383, 3392, 3394, 3396, 3398, 3400, 3414, 3415, 3417, 3429,     
        3434, 3436, 3438, 3441, 3463, 3467, 3465, 3478, 3480, 3482,     
        3483, 3484, 3485, 3487, 3488, 3489, 3490, 3491, 3492, 3503,     
        3506, 3510, 3512, 3513, 3514, 3515, 3516, 3517, 3518, 3519,     
        3520, 3522, 3523, 3524, 3525, 931, 932, 950, 2860, 4144,        
        4145, 4146, 4394, 4395, 4418, 4420, 4443, 4422, 4426, 4406,     
        4409, 4412, 4415, 4417, 4489, 4482, 828, 793, 804, 817,         
        825, 790, 801, 812, 795, 820, 809, 831, 930, 934,               
        936, 942, 945, 1004, 1006, 1009, 1034, 4863, 4856, 4862,        
        4557, 4561, 4562, 4563, 4564, 4568, 4569,                       
    };
    private static final int COMBAT_PARAM_ID = 1306;
    private static final int COMBAT_PARAM_NAME = 1308;
    private static final int COMBAT_PARAM_TIER = 1310;
    private static final int JOURNAL_GROUP = 741;
    private static final int JOURNAL_TITLE_CHILD = 2;
    private static final int JOURNAL_CONTENT_CHILD = 3;
    private static final String[] KARAMJA_EASY_NAMES = {
        "Pick 5 bananas from the plantation east of the volcano",
        "Use the rope swing to reach Moss Giant Island north-west of Karamja",
        "Mine gold from the rocks on the north-west peninsula",
        "Travel to Port Sarim by boat from Musa Point",
        "Travel to Ardougne by charter ship from Musa Point",
        "Explore Cairn Island to the west of Karamja",
        "Fish at the south end of Karamja island",
        "Collect 5 seaweed from Karamja",
        "Enter the TzHaar Fight Cave",
        "Kill a Jogre in Pothole Dungeon",
    };
    private static final int[] KARAMJA_EASY_VARBITS = {3566, 3567, 3568, 3569, 3570, 3571, 3572, 3573, 3574, 3575};
    private static final String[] KARAMJA_MEDIUM_NAMES = {
        "Claim a ticket from the Agility Arena in Brimhaven",
        "Discover the hidden wall in the dungeon below the volcano",
        "Visit the Isle of Crandor via the dungeon below the volcano",
        "Use Vigroy and Hajedy's cart service",
        "Earn 100% favour in the Tai Bwo Wannai Cleanup",
        "Cook a spider on a stick",
        "Mine a red topaz from a gem rock",
        "Cut a log from a teak tree",
        "Cut a log from a mahogany tree",
        "Catch a karambwan",
        "Exchange gems for a machete with Gabooty",
        "Use the gnome glider to travel to Karamja",
        "Grow a healthy fruit tree in the patch near Brimhaven",
        "Trap a horned graahk",
        "Chop the vines to gain access to Brimhaven Dungeon",
        "Cross the lava using stepping stones within Brimhaven Dungeon",
        "Climb the stairs within Brimhaven Dungeon",
        "Charter the Lady of the Waves from Cairn Isle to Port Khazard",
        "Charter a ship from the shipyard in the far east of Karamja",
    };
    private static final int[] KARAMJA_MEDIUM_VARBITS = {
        3579, 3580, 3581, 3582, 3583, 3584, 3585, 3586, 3587, 3588,
        3589, 3590, 3591, 3592, 3593, 3594, 3595, 3596, 3597,
    };
    private static final String[] KARAMJA_HARD_NAMES = {
        "Become the champion of the Fight Pits",
        "Kill TzTok-Jad in the TzHaar Fight Cave",
        "Eat an Oomlie wrap",
        "Craft some nature runes from essence",
        "Cook a karambwan thoroughly",
        "Kill a deathwing in the dungeon under the Kharazi Jungle",
        "Use the crossbow shortcut south of the volcano",
        "Collect 5 palm leaves",
        "Be assigned a Slayer task by Duradel in Shilo Village",
        "Kill a metal dragon in Brimhaven Dungeon",
    };
    private static final int[] KARAMJA_HARD_VARBITS = {3600, 3601, 3602, 3603, 3604, 3605, 3606, 3607, 3608, 3609};

    private final Client client;
    private final Map<String, Map<String, JsonArray>> diaryJournalCache = new HashMap<>();
    private final List<CombatTask> combatTasks = new ArrayList<>();
    private boolean checkedCombatTaskCache;

    RunePathAchievements(Client client)
    {
        this.client = client;
    }

    void reset()
    {
        diaryJournalCache.clear();
        combatTasks.clear();
        checkedCombatTaskCache = false;
    }

    JsonObject captureDiaries()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        JsonArray areas = new JsonArray();
        for (int areaIndex = 0; areaIndex < DIARY_AREAS.length; areaIndex++)
        {
            JsonObject area = new JsonObject();
            String areaName = DIARY_AREAS[areaIndex];
            area.addProperty("name", areaName);
            JsonArray tiers = new JsonArray();
            for (int tierIndex = 0; tierIndex < DIARY_TIERS.length; tierIndex++)
            {
                String tierName = DIARY_TIERS[tierIndex];
                int done = client.getVarbitValue(DIARY_COUNT_VARBITS[areaIndex][tierIndex]);
                boolean complete = client.getVarbitValue(DIARY_COMPLETE_VARBITS[areaIndex][tierIndex]) == 1;
                // A negative or impossibly large count means this dataset may not be ready.
                if (done < 0 || done > 200)
                {
                    return null;
                }
                JsonObject tier = new JsonObject();
                tier.addProperty("name", tierName);
                tier.addProperty("complete", complete);
                tier.addProperty("tasksDone", done);
                JsonArray tasks = null;
                if (areaIndex == 5 && tierIndex < 3)
                {
                    tasks = captureKaramjaTasks(tierIndex);
                }
                else
                {
                    Map<String, JsonArray> cached = diaryJournalCache.get(areaName);
                    if (cached != null) tasks = cached.get(tierName);
                }
                if (tasks != null && tasks.size() > 0) tier.add("tasks", tasks.deepCopy());
                tiers.add(tier);
            }
            area.add("tiers", tiers);
            areas.add(area);
        }

        JsonObject result = new JsonObject();
        result.addProperty("mode", "full");
        result.add("areas", areas);
        return result;
    }

    private JsonArray captureKaramjaTasks(int tier)
    {
        final String[] names = tier == 0 ? KARAMJA_EASY_NAMES
            : tier == 1 ? KARAMJA_MEDIUM_NAMES : KARAMJA_HARD_NAMES;
        final int[] vars = tier == 0 ? KARAMJA_EASY_VARBITS
            : tier == 1 ? KARAMJA_MEDIUM_VARBITS : KARAMJA_HARD_VARBITS;

        JsonArray tasks = new JsonArray();
        for (int i = 0; i < names.length; i++)
        {
            JsonObject task = new JsonObject();
            task.addProperty("name", names[i]);
            task.addProperty("complete", client.getVarbitValue(vars[i]) > 0);
            tasks.add(task);
        }
        return tasks;
    }

    JsonObject captureCombatAchievements()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;
        if (!checkedCombatTaskCache)
        {
            loadCombatTaskCache();
            // Game-cache structs may not all be ready right after login.
            // Retry on a later sync instead of permanently skipping achievements.
            checkedCombatTaskCache = combatTasks.size() == COMBAT_STRUCT_IDS.length;
        }

        // Prefer omitting an incomplete capture rather than sending task totals
        // or false-negative completion states when the game cache changes.
        if (combatTasks.size() != COMBAT_STRUCT_IDS.length) return null;

        JsonArray tiers = new JsonArray();
        int[] taskCount = new int[COMBAT_TIERS.length];
        int[] completed = new int[COMBAT_TIERS.length];
        JsonArray[] lists = new JsonArray[COMBAT_TIERS.length];
        for (int i = 0; i < lists.length; i++) lists[i] = new JsonArray();

        for (CombatTask task : combatTasks)
        {
            int tier = task.tier - 1;
            int varpIndex = task.id / 32;
            boolean done = taskComplete(task.id, client.getVarpValue(COMBAT_TASK_VARPS[varpIndex]));
            JsonObject row = new JsonObject();
            row.addProperty("id", task.id);
            row.addProperty("name", task.name);
            row.addProperty("complete", done);
            lists[tier].add(row);
            taskCount[tier]++;
            if (done) completed[tier]++;
        }

        for (int i = 0; i < COMBAT_TIERS.length; i++)
        {
            if (taskCount[i] == 0) return null;
            JsonObject tier = new JsonObject();
            tier.addProperty("name", COMBAT_TIERS[i]);
            tier.addProperty("complete", client.getVarbitValue(COMBAT_TIER_FLAGS[i]) == 1);
            tier.addProperty("tasksDone", completed[i]);
            tier.addProperty("tasksTotal", taskCount[i]);
            tier.add("tasks", lists[i]);
            tiers.add(tier);
        }
        JsonObject result = new JsonObject();
        result.addProperty("mode", "full");
        result.add("tiers", tiers);
        return result;
    }

    static boolean taskComplete(int taskId, int varpValue)
    {
        return (varpValue & (1 << (taskId % 32))) != 0;
    }

    private void loadCombatTaskCache()
    {
        combatTasks.clear();
        for (int structId : COMBAT_STRUCT_IDS)
        {
            StructComposition struct = client.getStructComposition(structId);
            if (struct == null) return;
            int id = struct.getIntValue(COMBAT_PARAM_ID);
            int tier = struct.getIntValue(COMBAT_PARAM_TIER);
            String name = struct.getStringValue(COMBAT_PARAM_NAME);
            if (id < 0 || id / 32 >= COMBAT_TASK_VARPS.length ||
                tier < 1 || tier > COMBAT_TIERS.length || name == null || name.trim().isEmpty())
            {
                combatTasks.clear();
                return;
            }
            combatTasks.add(new CombatTask(id, name, tier));
        }
    }

    /**
     * A diary's named tasks are not all exposed as individual varbits.
     * The journal is user-opened and may be incomplete until viewed. Its
     * task list is optional; completion and task counts come from varbits.
     */
    void observeDiaryJournal()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        Widget titleWidget = client.getWidget(JOURNAL_GROUP, JOURNAL_TITLE_CHILD);
        Widget content = client.getWidget(JOURNAL_GROUP, JOURNAL_CONTENT_CHILD);
        if (titleWidget == null || content == null) return;
        String header = normalise(titleWidget.getText());
        if (!header.startsWith("ACHIEVEMENT_DIARY")) return;
        Widget[] widgets = content.getStaticChildren();
        if (widgets == null || widgets.length == 0) widgets = content.getDynamicChildren();
        if (widgets == null || widgets.length == 0) return;

        String area = null;
        for (Widget widget : widgets)
        {
            if (widget == null) continue;
            String name = normalise(widget.getText());
            for (String candidate : DIARY_AREAS)
            {
                String normalizedCandidate = normalise(candidate + " Tasks");
                if (name.equals(normalizedCandidate) ||
                    name.equals(normalise(candidate + " Area Tasks")) ||
                    (candidate.equals("Western Provinces") && name.equals("WESTERN_AREA_TASKS")) ||
                    (candidate.equals("Lumbridge & Draynor") && name.equals("LUMBRIDGE_&_DRAYNOR_TASKS".replace('&', '_'))))
                {
                    area = candidate;
                    break;
                }
            }
            if (area != null) break;
        }
        if (area == null) return;

        Map<String, JsonArray> captured = new HashMap<>();
        for (String tierName : DIARY_TIERS) captured.put(tierName, new JsonArray());
        String activeTier = null;
        for (Widget widget : widgets)
        {
            if (widget == null || widget.getText() == null) continue;
            String raw = widget.getText();
            for (String fragment : raw.replace('\r','\n').split("(?i)<br\\s*/?>|\\n"))
            {
                String name = stripMarkup(fragment).trim();
                if (name.isEmpty()) continue;
                String upper = name.toUpperCase(Locale.ROOT);
                if (upper.startsWith("EASY")) { activeTier = "easy"; continue; }
                if (upper.startsWith("MEDIUM")) { activeTier = "medium"; continue; }
                if (upper.startsWith("HARD")) { activeTier = "hard"; continue; }
                if (upper.startsWith("ELITE")) { activeTier = "elite"; continue; }
                if (activeTier == null || name.length() < 8 ||
                    upper.contains("_TASKS") || name.toLowerCase(Locale.ROOT).startsWith("if i ever lose my "))
                {
                    continue;
                }
                // Keep only recognized journal lines, never invent missing tasks.
                JsonObject item = new JsonObject();
                item.addProperty("name", name);
                item.addProperty("complete", fragment.toLowerCase(Locale.ROOT).contains("<str>"));
                captured.get(activeTier).add(item);
            }
        }
        if (captured.values().stream().anyMatch(list -> list.size() > 0))
        {
            diaryJournalCache.put(area, captured);
        }
    }

    private static String stripMarkup(String value)
    {
        return value == null ? "" : value.replaceAll("<[^>]*>", "");
    }

    private static String normalise(String value)
    {
        return stripMarkup(value).trim().toUpperCase(Locale.ROOT)
            .replace(' ', '_').replace('&', '_').replaceAll("_+", "_");
    }

    private static final class CombatTask
    {
        final int id;
        final String name;
        final int tier;
        CombatTask(int id, String name, int tier)
        {
            this.id = id;
            this.name = name;
            this.tier = tier;
        }
    }
}
