package dev.rlcraftceaddons.thirstpreview.client;

import com.charles445.simpledifficulty.api.config.QuickConfig;
import com.charles445.simpledifficulty.client.gui.ThirstGui;
import com.charles445.simpledifficulty.config.ModConfig;
import dev.rlcraftceaddons.thirstpreview.ThirstPreviewConfig;
import dev.rlcraftceaddons.thirstpreview.ThirstValues;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.lwjgl.opengl.GL11;

/** Uses a measured row inside the vanilla tooltip, never AppleSkin's outside panel. */
public final class TooltipHydrationRenderer {
    static Integer renderedAmount(RenderTooltipEvent event) {
        // List-only containers can legitimately provide ItemStack.EMPTY here.
        // The owned row carries the value resolved when this tooltip was built.
        return TooltipHydrationSnapshot.decode(event.getLines());
    }

    public void clearRows(List<String> lines) {
        TooltipHydrationSnapshot.removeRows(lines);
    }

    public boolean addRow(List<String> lines, ItemStack stack, ThirstValues values) {
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = Minecraft.getMinecraft().fontRenderer;
        String reserve = reservation(font, values.amount);
        TooltipHydrationSnapshot.upsert(lines, reserve);
        return reserve != null;
    }

    public void post(RenderTooltipEvent.PostText event) {
        // Keep native rendering and wrapping intact, including container footers.
        if (!enabled()) return;
        int index = TooltipHydrationLayout.findRow(event.getLines());
        if (index < 0) return;
        Integer amount = renderedAmount(event);
        if (amount == null) return;
        FontRenderer font = event.getFontRenderer();
        TooltipHydrationLayout.Row row = TooltipHydrationLayout.forAmount(amount);
        int y = TooltipHydrationLayout.iconTop(event.getY(), event.getHeight(), index, event.getLines().size());
        if (y < 0) return; // An over-height/clipped tooltip must not paint over a different line.
        int rowWidth = row.width(0);
        ScaledResolution screen = new ScaledResolution(Minecraft.getMinecraft());
        boolean icons = TooltipHydrationLayout.complete(event.getLines().get(index), reservation(font, amount))
                && fits(event, screen, y, rowWidth);
        if (!icons) return;
        try (PreviewRenderState ignored = PreviewRenderState.capture()) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.getTextureManager().bindTexture(ModConfig.client.classicHUDThirst ? ThirstGui.ICONS : ThirstGui.THIRSTHUD);
            if (amount == 0) GlStateManager.color(0.65F, 0.65F, 0.65F, 1F);
            else GlStateManager.color(1F, row.loss ? 0.3F : 1F, row.loss ? 0.3F : 1F, 1F);
            int x = event.getX();
            for (int icon = 0; icon < row.iconCount; icon++) {
                int width = row.iconWidth(icon);
                int crop = 9 - width;
                minecraft.ingameGUI.drawTexturedModalRect(x + icon * 9 + crop, y,
                        row.textureU(icon) + crop, 0, width, 9);
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    private boolean fits(RenderTooltipEvent.PostText event, ScaledResolution screen, int y, int width) {
        return TooltipHydrationLayout.fits(event.getX(), y, width, 9, event.getX(), event.getY(),
                event.getWidth(), event.getHeight(), screen.getScaledWidth(), screen.getScaledHeight());
    }
    private String reservation(FontRenderer font, int amount) {
        return TooltipHydrationSnapshot.create(amount, rowWidth(font, amount), font.getStringWidth("\u00a0"));
    }
    private int rowWidth(FontRenderer font, int amount) {
        TooltipHydrationLayout.Row row = TooltipHydrationLayout.forAmount(amount);
        return row.width(0);
    }
    private boolean enabled() { return ThirstPreviewConfig.showTooltip && QuickConfig.isThirstEnabled(); }
}
