package dev.blackmon;

import net.runelite.client.config.*;
import java.awt.*;

@ConfigGroup("zulrahwristsaver")
public interface ZulrahWristSaverConfig extends Config
{
    @Alpha
    @ConfigItem(
            keyName = "outlineColor",
            name = "Outline color",
            description = "Color of the Zulrah outline when it becomes attackable.",
            position = 1
    )
    default Color outlineColor() { return new Color(0, 255, 0, 255); }

    @Range(min = 0, max = 12)
    @ConfigItem(
            keyName = "outlineFeather",
            name = "Outline feather",
            description = "Feather (soften) the edges of the outline.",
            position = 2
    )
    default int outlineFeather() { return 1; }
}
