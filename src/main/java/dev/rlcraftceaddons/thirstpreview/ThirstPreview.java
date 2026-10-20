package dev.rlcraftceaddons.thirstpreview;

import dev.rlcraftceaddons.thirstpreview.client.ClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = ThirstPreview.MODID, name = "Thirst Preview", version = "1.1.1",
        acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true,
        acceptableRemoteVersions = "*",
        dependencies = "required-after:simpledifficulty@[0.3.9,0.4);after:appleskin")
public final class ThirstPreview {
    public static final String MODID = "thirstpreview";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            ThirstPreviewConfig.load(event.getSuggestedConfigurationFile());
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (event.getSide().isClient()) {
            // Register after every mod's init, so our LOWEST Post(ALL) follows the bar owner.
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
        }
    }
}
