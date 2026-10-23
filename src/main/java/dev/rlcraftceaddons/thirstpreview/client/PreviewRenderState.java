package dev.rlcraftceaddons.thirstpreview.client;

import java.nio.FloatBuffer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

/** Scoped state for overlays; restore through Minecraft's cache, not raw GL setters. */
public final class PreviewRenderState implements AutoCloseable {
    private final int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final boolean textured = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    private final boolean rescale = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    // LWJGL 2 validates a sixteen-float minimum for glGetFloat queries.
    private final FloatBuffer color = BufferUtils.createFloatBuffer(16);
    private boolean closed;

    private PreviewRenderState() {
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
    }
    public static PreviewRenderState capture() { return new PreviewRenderState(); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(matrixMode);
        GlStateManager.bindTexture(texture);
        GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
        GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        GlStateManager.depthMask(depthMask);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (textured) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (rescale) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal();
    }
}
