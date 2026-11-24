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
        description = "Only the published RunePath HTTPS origin (https://runepath.floot.app) is accepted by this development build.",
        position = 0
    )
    default String serverUrl()
    {
        return "https://runepath.floot.app";
    }

    @ConfigItem(
        keyName = "allowProgressSync",
        name = "Enable third-party progress sync",
        description = "Opt in to sending your OSRS character name, skill levels/XP, quests, achievement diaries and combat achievements to the RunePath HTTPS server.",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 1
    )
    default boolean allowProgressSync()
    {
        return false;
    }
}
