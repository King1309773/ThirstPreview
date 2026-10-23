package dev.rlcraftceaddons.thirstpreview.client;

import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.api.SDCompatibility;
import com.charles445.simpledifficulty.api.SDPotions;
import com.charles445.simpledifficulty.api.config.QuickConfig;
import com.charles445.simpledifficulty.api.thirst.IThirstCapability;
import com.charles445.simpledifficulty.client.gui.ThirstGui;
import com.charles445.simpledifficulty.config.ModConfig;
import dev.rlcraftceaddons.thirstpreview.PreviewMath;
import dev.rlcraftceaddons.thirstpreview.ThirstPreviewConfig;
import dev.rlcraftceaddons.thirstpreview.ThirstValueResolver;
import dev.rlcraftceaddons.thirstpreview.ThirstValues;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Client-only presentation; never changes the capability or consumption behavior. */
@SideOnly(Side.CLIENT)
public final class ClientEvents {
    private final GraveStoneBarPreview barPreview = new GraveStoneBarPreview();
    // LWJGL 2 requires room for sixteen floats even for GL_CURRENT_COLOR.
    private final FloatBuffer savedColor = BufferUtils.createFloatBuffer(16);
    private int updateCounter;
    private int capturedThirstY;
    private boolean capturedNativeRow;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemTooltip(ItemTooltipEvent event) {
        TooltipHydrationSnapshot.removeRows(event.getToolTip());
        if (!ThirstPreviewConfig.showTooltip || !QuickConfig.isThirstEnabled()) {
            return;
        }
        ThirstValues values = ThirstValueResolver.resolve(event.getItemStack());
        if (values == null) {
            return;
        }
        boolean details = !ThirstPreviewConfig.showDetailsWithShift || GuiScreen.isShiftKeyDown();
        event.getToolTip().add(TextFormatting.AQUA + I18n.format(
                "thirstpreview.tooltip.points", Integer.toString(values.amount)));
        if (details) {
            event.getToolTip().add(TextFormatting.GRAY + I18n.format(
                    "thirstpreview.tooltip.saturation", format(values.saturation)));
            event.getToolTip().add(TextFormatting.GRAY + I18n.format(
                    "thirstpreview.tooltip.dirty_chance", format(values.dirtyChance * 100F)));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !Minecraft.getMinecraft().isGamePaused()) {
            // SimpleDifficulty uses this same counter for its low-thirst shake.
            updateCounter++;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            capturedNativeRow = false;
            barPreview.beginFrame();
            return;
        }
        if (event.getType() != RenderGameOverlayEvent.ElementType.AIR) {
            return;
        }
        capturedNativeRow = false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.isCanceled() || !shouldRender(minecraft)) {
            return;
        }
        // ThirstGui ran at NORMAL and has already reserved ten pixels. Capture
        // now: vanilla AIR may reserve another ten underwater before AIR Post.
        capturedThirstY = ClientPreviewPolicy.thirstY(
                event.getResolution().getScaledHeight(), GuiIngameForge.right_height);
        capturedNativeRow = true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void captureHorizontalBar(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            barPreview.capture(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(),
                    commonVisible(Minecraft.getMinecraft()), SDCompatibility.defaultThirstDisplay);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (commonVisible(minecraft)) {
                ThirstValues values = heldValues(minecraft);
                IThirstCapability thirst = SDCapabilities.getThirstData(minecraft.player);
                if (values != null && thirst != null) {
                    int current = thirst.getThirstLevel();
                    barPreview.render(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(),
                            current, PreviewMath.restoredLevel(current, values.amount), pulse(event.getPartialTicks()));
                }
            }
            return;
        }
        if (event.getType() != RenderGameOverlayEvent.ElementType.AIR || !capturedNativeRow) {
            return;
        }
        capturedNativeRow = false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!shouldRender(minecraft)) {
            return;
        }
        ThirstValues values = heldValues(minecraft);
        if (values == null) {
            return;
        }
        IThirstCapability thirst = SDCapabilities.getThirstData(minecraft.player);
        if (thirst == null) {
            return;
        }
        int current = thirst.getThirstLevel();
        int restored = PreviewMath.restoredLevel(current, values.amount);
        if (current == restored) {
            return;
        }
        int[] shakeOffsets = ClientPreviewPolicy.nativeYOffsets(
                updateCounter, current, thirst.getThirstSaturation());
        boolean thirsty = SDPotions.thirsty != null && minecraft.player.isPotionActive(SDPotions.thirsty);
        float alpha = pulse(event.getPartialTicks());
        renderChangedIcons(minecraft, event.getResolution().getScaledWidth(),
                current, restored, shakeOffsets, thirsty, alpha);
    }

    private boolean shouldRender(Minecraft minecraft) {
        boolean nativeHud = commonVisible(minecraft) && GuiIngameForge.renderAir;
        return ClientPreviewPolicy.shouldShowHud(ThirstPreviewConfig.showHud,
                minecraft.gameSettings.hideGUI, nativeHud, QuickConfig.isThirstEnabled(),
                SDCompatibility.defaultThirstDisplay);
    }

    private boolean commonVisible(Minecraft minecraft) {
        return ThirstPreviewConfig.showHud && QuickConfig.isThirstEnabled() && !minecraft.gameSettings.hideGUI
                && minecraft.player != null && minecraft.world != null && minecraft.currentScreen == null
                && !minecraft.player.capabilities.isCreativeMode && !minecraft.player.isSpectator()
                && minecraft.playerController != null && minecraft.playerController.shouldDrawHUD()
                && minecraft.getRenderViewEntity() == minecraft.player;
    }

    private ThirstValues heldValues(Minecraft minecraft) {
        return ClientPreviewPolicy.resolveHeld(
                minecraft.player.isHandActive() ? minecraft.player.getActiveItemStack() : null,
                minecraft.player.getHeldItemMainhand(), minecraft.player.getHeldItemOffhand(), ThirstValueResolver::resolve);
    }

    private float pulse(float partialTicks) {
        return 0.4F + 0.3F * ((float) Math.sin((updateCounter + partialTicks) * 0.25F) + 1F);
    }

    private void renderChangedIcons(Minecraft minecraft, int scaledWidth, int current, int restored,
                                    int[] shakeOffsets, boolean thirsty, float alpha) {
        boolean wasBlending = GL11.glIsEnabled(GL11.GL_BLEND);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        savedColor.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, savedColor);
        try {
            minecraft.getTextureManager().bindTexture(
                    ModConfig.client.classicHUDThirst ? ThirstGui.ICONS : ThirstGui.THIRSTHUD);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
            for (int slot = 0; slot < 10; slot++) {
                HudIconChange change = HudIconChange.between(
                        PreviewMath.fillForSlot(current, slot), PreviewMath.fillForSlot(restored, slot));
                if (change == null) {
                    continue;
                }
                GlStateManager.color(1F, change.gain ? 1F : 0.3F, change.gain ? 1F : 0.3F, alpha);
                minecraft.ingameGUI.drawTexturedModalRect(
                        ClientPreviewPolicy.dropletX(scaledWidth, slot),
                        capturedThirstY + shakeOffsets[slot],
                        change.textureU + (thirsty ? 36 : 0), 0, change.width, 9);
            }
        } finally {
            // Restore through GlStateManager so its cached state remains accurate.
            GlStateManager.bindTexture(previousTexture);
            GlStateManager.color(savedColor.get(0), savedColor.get(1), savedColor.get(2), savedColor.get(3));
            GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            if (wasBlending) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }
        }
    }

    private static String format(float value) {
        return new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }
}
