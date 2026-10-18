package dev.rlcraftceaddons.thirstpreview.client;

import com.charles445.simpledifficulty.api.config.QuickConfig;
import com.charles445.simpledifficulty.client.gui.ThirstGui;
import com.charles445.simpledifficulty.config.ModConfig;
import dev.rlcraftceaddons.thirstpreview.ThirstPreviewConfig;
import dev.rlcraftceaddons.thirstpreview.ThirstValueResolver;
import dev.rlcraftceaddons.thirstpreview.ThirstValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;

/** Uses a measured row inside the vanilla tooltip, never AppleSkin's outside panel. */
public final class TooltipHydrationRenderer {
    private boolean drawingFallback;

    public boolean addRow(List<String> lines, ItemStack stack, ThirstValues values) {
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = Minecraft.getMinecraft().fontRenderer;
        String reserve = reservation(font, values.amount);
        if (reserve == null) return false;
        lines.add(reserve);
        return true;
    }

    /** Rerender through Forge with readable native text when a strip cannot fit. */
    public void pre(RenderTooltipEvent.Pre event) {
        if (drawingFallback || !enabled()) return;
        boolean marked = event.getLines().stream().anyMatch(line -> line.contains(TooltipHydrationLayout.MARKER));
        if (!marked) return;
        ThirstValues values = ThirstValueResolver.resolve(event.getStack());
        if (values == null) return;
        FontRenderer font = event.getFontRenderer();
        String expected = reservation(font, values.amount);
        ScaledResolution screen = new ScaledResolution(Minecraft.getMinecraft());
        if (event.getScreenWidth() == screen.getScaledWidth() && event.getScreenHeight() == screen.getScaledHeight()
                && TooltipHydrationLayout.preflight(event.getLines(), expected, rowWidth(font, values.amount),
                    event.getX(), event.getY(), event.getScreenWidth(), event.getScreenHeight(), event.getMaxWidth(),
                    new TooltipHydrationLayout.Metrics() {
                        public int width(String text) { return font.getStringWidth(text); }
                        public List<String> wrap(String text, int width) { return font.listFormattedStringToWidth(text, width); }
                    })) return;
        List<String> fallback = new ArrayList<>(event.getLines());
        for (int index = 0; index < fallback.size(); index++) {
            if (fallback.get(index).contains(TooltipHydrationLayout.MARKER)) {
                fallback.set(index, "§b" + I18n.format("thirstpreview.tooltip.points", values.amount));
            }
        }
        event.setCanceled(true);
        drawingFallback = true;
        try {
            GuiUtils.drawHoveringText(event.getStack(), fallback, event.getX(), event.getY(),
                    event.getScreenWidth(), event.getScreenHeight(), event.getMaxWidth(), font);
        } finally {
            drawingFallback = false;
        }
    }

    public void post(RenderTooltipEvent.PostText event) {
        if (drawingFallback || !enabled()) return;
        int index = TooltipHydrationLayout.findRow(event.getLines());
        if (index < 0) return;
        ThirstValues values = ThirstValueResolver.resolve(event.getStack());
        if (values == null) return;
        FontRenderer font = event.getFontRenderer();
        TooltipHydrationLayout.Row row = TooltipHydrationLayout.forAmount(values.amount);
        int y = TooltipHydrationLayout.iconTop(event.getY(), event.getHeight(), index, event.getLines().size());
        if (y < 0) return; // An over-height/clipped tooltip must not paint over a different line.
        String label = I18n.format("thirstpreview.tooltip.label");
        int labelWidth = font.getStringWidth(label);
        int rowWidth = labelWidth + 3 + row.width(font.getStringWidth(row.counter));
        ScaledResolution screen = new ScaledResolution(Minecraft.getMinecraft());
        boolean icons = TooltipHydrationLayout.complete(event.getLines().get(index), reservation(font, values.amount))
                && fits(event, screen, y, rowWidth);
        String fallback = I18n.format("thirstpreview.tooltip.points", values.amount);
        if (!icons && !fits(event, screen, y, font.getStringWidth(fallback))) {
            fallback = Integer.toString(values.amount);
            if (!fits(event, screen, y, font.getStringWidth(fallback))) return;
        }
        try (PreviewRenderState ignored = PreviewRenderState.capture()) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            if (!icons) {
                font.drawStringWithShadow(fallback, event.getX(), y + 1, 0x55FFFF);
                return;
            }
            font.drawStringWithShadow(label, event.getX(), y + 1, 0x55FFFF);
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.getTextureManager().bindTexture(ModConfig.client.classicHUDThirst ? ThirstGui.ICONS : ThirstGui.THIRSTHUD);
            if (values.amount == 0) GlStateManager.color(0.65F, 0.65F, 0.65F, 1F);
            else GlStateManager.color(1F, row.loss ? 0.3F : 1F, row.loss ? 0.3F : 1F, 1F);
            int x = event.getX() + labelWidth + 3;
            for (int icon = 0; icon < row.iconCount; icon++) {
                minecraft.ingameGUI.drawTexturedModalRect(x + icon * 9, y, row.textureU(icon), 0, 9, 9);
            }
            GlStateManager.color(1F, 1F, 1F, 1F);
            if (!row.counter.isEmpty()) font.drawStringWithShadow(row.counter,
                    x + row.iconCount * 9 + 3, y + 1, row.loss ? 0xFF6666 : (values.amount == 0 ? 0xAAAAAA : 0x55FFFF));
        }
    }

    private boolean fits(RenderTooltipEvent.PostText event, ScaledResolution screen, int y, int width) {
        return TooltipHydrationLayout.fits(event.getX(), y, width, 9, event.getX(), event.getY(),
                event.getWidth(), event.getHeight(), screen.getScaledWidth(), screen.getScaledHeight());
    }
    private String reservation(FontRenderer font, int amount) {
        return TooltipHydrationLayout.reservation(rowWidth(font, amount), font.getStringWidth("\u00a0"));
    }
    private int rowWidth(FontRenderer font, int amount) {
        TooltipHydrationLayout.Row row = TooltipHydrationLayout.forAmount(amount);
        return font.getStringWidth(I18n.format("thirstpreview.tooltip.label")) + 3 + row.width(font.getStringWidth(row.counter));
    }
    private boolean enabled() { return ThirstPreviewConfig.showTooltip && QuickConfig.isThirstEnabled(); }
}
