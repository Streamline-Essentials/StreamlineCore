package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.streamline.client.Canvas;
import org.lwjgl.opengl.GL11;

/**
 * {@link Canvas} over Minecraft 1.8.9's immediate-mode GUI: {@link Gui#drawRect} and the
 * font renderer, transformed with {@link GlStateManager}'s matrix stack.
 *
 * <p>Clipping uses the GL scissor test, which works in window pixels with the origin at the
 * bottom left, so it does not follow the matrix stack; callers clip in screen coordinates.</p>
 */
public final class GfxCanvas implements Canvas {

    private final Minecraft minecraft;
    private final FontRenderer font;
    private final ScaledResolution resolution;

    private GfxCanvas(ScaledResolution resolution) {
        this.minecraft = Minecraft.getMinecraft();
        this.font = minecraft.fontRendererObj;
        this.resolution = resolution;
    }

    public static GfxCanvas wrap(ScaledResolution resolution) {
        return new GfxCanvas(resolution);
    }

    /** A canvas for the current window size. */
    public static GfxCanvas current() {
        return new GfxCanvas(new ScaledResolution(Minecraft.getMinecraft()));
    }

    @Override
    public int width() {
        return resolution.getScaledWidth();
    }

    @Override
    public int height() {
        return resolution.getScaledHeight();
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
        // drawRect leaves its color on the GL state, which would tint the next text drawn.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void text(String text, int x, int y, int color, boolean shadow) {
        GlStateManager.enableBlend();
        font.drawString(LegacyCodes.convert(text), (float) x, (float) y, color, shadow);
    }

    @Override
    public int textWidth(String text) {
        return font.getStringWidth(LegacyCodes.convert(text));
    }

    /** 1.8.9's font renderer applies only {@code §} codes, so that is the one character to hide. */
    @Override
    public void rawText(String text, int x, int y, int color) {
        GlStateManager.enableBlend();
        font.drawString(text.replace('§', '&'), (float) x, (float) y, color, false);
    }

    @Override
    public int rawTextWidth(String text) {
        return font.getStringWidth(text.replace('§', '&'));
    }

    @Override
    public int lineHeight() {
        return font.FONT_HEIGHT;
    }

    @Override
    public void push() {
        GlStateManager.pushMatrix();
    }

    @Override
    public void pop() {
        GlStateManager.popMatrix();
    }

    @Override
    public void translate(float x, float y) {
        GlStateManager.translate(x, y, 0.0F);
    }

    @Override
    public void scale(float factor) {
        GlStateManager.scale(factor, factor, 1.0F);
    }

    @Override
    public void clip(int x1, int y1, int x2, int y2) {
        int factor = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x1 * factor, minecraft.displayHeight - y2 * factor,
                Math.max(0, (x2 - x1) * factor), Math.max(0, (y2 - y1) * factor));
    }

    @Override
    public void endClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
