package dev.rlcraftceaddons.thirstpreview;

import dev.rlcraftceaddons.thirstpreview.client.ClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = ThirstPreview.MODID, name = "Thirst Preview", version = "1.0.0",
        acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true,
        acceptableRemoteVersions = "*",
        dependencies = "required-after:simpledifficulty@[0.3.9,0.4);after:appleskin")
public final class ThirstPreview {
    public static final String MODID = "thirstpreview";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            ThirstPreviewConfig.load(event.getSuggestedConfigurationFile());
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
        }
    }
}
