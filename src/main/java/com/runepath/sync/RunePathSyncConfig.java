package com.runepath.sync;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("runepathsync")
public interface RunePathSyncConfig extends Config
{
    @ConfigItem(
        keyName = "serverUrl",
        name = "RunePath HTTPS URL",
        description = "The published RunePath HTTPS address; default is https://runepath.floot.app. Do not enter preview links.",
        position = 0
    )
    default String serverUrl()
    {
        return "https://runepath.floot.app";
    }

    @ConfigItem(
        keyName = "allowProgressSync",
        name = "Enable third-party progress sync",
        description = "Opt in to sending your IP address, OSRS character name, skill levels/XP, quests, achievement diary status and combat achievement task progress to a third-party RunePath server not verified or controlled by RuneLite developers.",
        position = 1
    )
    default boolean allowProgressSync()
    {
        return false;
    }
}
