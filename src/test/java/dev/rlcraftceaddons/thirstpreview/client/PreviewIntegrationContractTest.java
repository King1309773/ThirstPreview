package dev.rlcraftceaddons.thirstpreview.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** Headless wiring checks, not a substitute for in-game OpenGL validation. */
public class PreviewIntegrationContractTest {
    private String source(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/dev/rlcraftceaddons/thirstpreview/" + name)), StandardCharsets.UTF_8);
    }

    @Test public void eventsUseBothIndependentRenderers() throws Exception {
        String text = source("client/ClientEvents.java");
        assertTrue("tooltip icons must be wired", text.contains("TooltipHydrationRenderer"));
        assertTrue("horizontal preview must be wired", text.contains("barPreview.capture("));
        assertTrue(text.contains("barPreview.beginFrame("));
        assertTrue(text.contains("barPreview.render("));
        assertTrue("menus must suppress held preview", text.contains("minecraft.currentScreen == null"));
        assertTrue("do not put the local player's hydration on another view player's bar",
                text.contains("minecraft.getRenderViewEntity() == minecraft.player"));
    }

    @Test public void listenerRegistrationWaitsUntilAfterOtherModsInit() throws Exception {
        String text = source("ThirstPreview.java");
        assertTrue(text.contains("FMLPostInitializationEvent"));
        assertFalse(text.substring(text.indexOf("public void preInit"), text.indexOf("public void postInit")).contains("EVENT_BUS.register"));
    }

    @Test public void stateGuardRestoresStateThroughMinecraftCache() throws Exception {
        java.nio.file.Path path = Paths.get("src/main/java/dev/rlcraftceaddons/thirstpreview/client/PreviewRenderState.java");
        assertTrue("shared render guard is missing", Files.isRegularFile(path));
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(text.contains("implements AutoCloseable"));
        assertTrue(text.contains("GlStateManager.popMatrix()"));
        assertTrue(text.contains("GlStateManager.bindTexture(texture)"));
        assertTrue(text.contains("GlStateManager.tryBlendFuncSeparate"));
        assertTrue(text.contains("GlStateManager.depthMask(depthMask)"));
        assertTrue("FontRenderer enables alpha testing; restore that flag too", text.contains("GlStateManager.disableAlpha()"));
    }
}
