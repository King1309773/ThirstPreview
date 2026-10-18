package dev.rlcraftceaddons.thirstpreview;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class ThirstPreviewConfig {
    public static boolean showTooltip = true;
    public static boolean showHud = true;
    public static boolean showDetailsWithShift = true;

    private ThirstPreviewConfig() { }

    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        showTooltip = config.getBoolean("showTooltip", "client", true,
                "Show per-use hydration in item tooltips (inventory and JEI).");
        showHud = config.getBoolean("showHud", "client", true,
                "Preview held food/drink hydration on the SimpleDifficulty thirst bar.");
        showDetailsWithShift = config.getBoolean("showDetailsWithShift", "client", true,
                "Show saturation and dehydration probability when holding Shift. False: always show details.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}
