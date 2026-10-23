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
                "Show per-use hydration points as text in item tooltips (inventory, containers and JEI).");
        showHud = config.getBoolean("showHud", "client", true,
                "Preview held hydration on the native thirst HUD or supported GraveStone water bar.");
        showDetailsWithShift = config.getBoolean("showDetailsWithShift", "client", true,
                "Show saturation and dehydration probability with Shift. Hydration points are always visible. False: always show details.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}
