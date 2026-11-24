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
        description = "Published RunePath app HTTPS origin; do not use a preview link.",
        position = 0
    )
    default String serverUrl()
    {
        return "";
    }

    @ConfigItem(
        keyName = "allowProgressSync",
        name = "Enable third-party progress sync",
        description = "Opt in to sending your IP address, OSRS character name, skill levels/XP and quest status to a third-party RunePath server not verified or controlled by RuneLite developers.",
        position = 1
    )
    default boolean allowProgressSync()
    {
        return false;
    }
}
